package com.example.bookapp.Repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.ChatMessage;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ChatRepository: Tích hợp trực tiếp Google Gemini AI và Cloud Firestore trên Android.
 * Hỗ trợ Agent Loop (Multi-turn Tool Calling) và thuật toán lọc sách thông minh.
 */
public class ChatRepository {

    private static final String TAG = "ChatRepository";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_TURNS = 5;
    private static final int MAX_HISTORY_TURNS = 20;

    private static final String SYSTEM_INSTRUCTION =
            "Bạn là Trợ lý Tư vấn Sách Thông minh của nhà sách Thunder Book.\n" +
            "Nhiệm vụ của bạn là lắng nghe nhu cầu của độc giả, tìm kiếm và tư vấn những cuốn sách phù hợp nhất.\n\n" +
            "QUY TẮC BẮT BUỘC:\n" +
            "1. CHỈ tư vấn và trích xuất dữ liệu dựa trên kết quả trả về từ các công cụ (search_books, check_stock, get_book_detail).\n" +
            "   - Tuyệt đối KHÔNG tự bịa đặt tên sách, giá, tác giả, đánh giá hoặc tình trạng tồn kho.\n" +
            "2. HƯỚNG DẪN CHỌN TOOL:\n" +
            "   - Dùng `search_books`: Khi khách hỏi tìm sách theo tên, tác giả, thể loại, khoảng giá, sách giảm giá, hoặc miêu tả chủ đề/nhu cầu/tâm trạng.\n" +
            "   - Dùng `check_stock`: Khi khách hỏi tình trạng còn hàng / số lượng tồn của 1 cuốn sách.\n" +
            "   - Dùng `get_book_detail`: Khi khách muốn xem chi tiết thông tin đầy đủ về 1 cuốn sách.\n" +
            "3. PHONG CÁCH TƯ VẤN:\n" +
            "   - Thân thiện, chu đáo, nhiệt tình, sử dụng tiếng Việt tự nhiên.\n" +
            "   - Nêu rõ giá khuyến mãi (salePrice), giá gốc (price), điểm đánh giá (Rating) khi tư vấn.\n" +
            "   - Nếu sách hết hàng (stock = 0), hãy thông báo rõ và gợi ý sách tương tự còn hàng.\n" +
            "   - Sau khi tra cứu công cụ có kết quả, luôn tóm tắt và đưa ra câu trả lời đầy đủ, chi tiết cho khách hàng.";

    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final List<JSONObject> conversationHistory = new ArrayList<>();

    public ChatRepository() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface ChatCallback {
        void onSuccess(ChatMessage botResponse);
        void onError(String errorMessage);
    }

    /**
     * Gửi tin nhắn đến Google Gemini AI trực tiếp từ Android.
     */
    public void sendMessage(String userId, String sessionId, String userMessage, ChatCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. Giới hạn độ dài lịch sử hội thoại để tránh tràn bộ nhớ/payload quá tải
                if (conversationHistory.size() > MAX_HISTORY_TURNS) {
                    while (conversationHistory.size() > MAX_HISTORY_TURNS) {
                        conversationHistory.remove(0);
                    }
                }

                // 2. Thêm tin nhắn user vào lịch sử hội thoại
                JSONObject userTurn = new JSONObject();
                userTurn.put("role", "user");
                JSONArray userParts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", userMessage);
                userParts.put(textPart);
                userTurn.put("parts", userParts);
                conversationHistory.add(userTurn);

                List<Book> allSuggestedBooks = new ArrayList<>();
                Set<String> addedBookIds = new HashSet<>();

                // 3. Vòng lặp Agent Multi-Turn Tool Calling
                int turnCount = 0;
                while (turnCount < MAX_TURNS) {
                    turnCount++;

                    JSONObject payload = buildGeminiPayload();
                    JSONObject geminiResponse = callGeminiApi(payload);

                    if (geminiResponse == null) {
                        postError(callback, "Không thể kết nối đến máy chủ AI (Google Gemini). Vui lòng thử lại sau.");
                        return;
                    }

                    JSONArray candidates = geminiResponse.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        postError(callback, "Dịch vụ AI phản hồi không có nội dung.");
                        return;
                    }

                    JSONObject candidate = candidates.getJSONObject(0);
                    JSONObject content = candidate.optJSONObject("content");
                    if (content == null) {
                        postError(callback, "Phản hồi AI không đúng định dạng.");
                        return;
                    }

                    JSONArray parts = content.optJSONArray("parts");
                    if (parts == null || parts.length() == 0) {
                        postError(callback, "Dịch vụ AI không trả về dữ liệu.");
                        return;
                    }

                    // Kiểm tra xem có FunctionCall nào không
                    List<JSONObject> functionCalls = new ArrayList<>();
                    StringBuilder textBuffer = new StringBuilder();

                    for (int i = 0; i < parts.length(); i++) {
                        JSONObject p = parts.getJSONObject(i);
                        if (p.has("functionCall")) {
                            functionCalls.add(p.getJSONObject("functionCall"));
                        }
                        if (p.has("text")) {
                            textBuffer.append(p.getString("text"));
                        }
                    }

                    // Trường hợp AI muốn gọi Function (Tool Calling)
                    if (!functionCalls.isEmpty()) {
                        // Thêm model turn vào lịch sử (giữ nguyên parts kèm thought signature)
                        JSONObject modelTurn = new JSONObject();
                        modelTurn.put("role", "model");
                        modelTurn.put("parts", parts);
                        conversationHistory.add(modelTurn);

                        // Thực thi từng function call và gom kết quả
                        JSONArray toolResponseParts = new JSONArray();
                        for (JSONObject fnCall : functionCalls) {
                            String functionName = fnCall.getString("name");
                            JSONObject args = fnCall.optJSONObject("args");
                            if (args == null) args = new JSONObject();

                            Log.d(TAG, "Gemini yêu cầu gọi function [" + functionName + "] với args: " + args);

                            List<Book> turnBooks = new ArrayList<>();
                            JSONObject toolResult = executeFirestoreTool(functionName, args, turnBooks);

                            // Gom sách gợi ý
                            for (Book b : turnBooks) {
                                if (b.getBookId() != null && !addedBookIds.contains(b.getBookId())) {
                                    addedBookIds.add(b.getBookId());
                                    allSuggestedBooks.add(b);
                                }
                            }

                            // Tạo functionResponse part
                            JSONObject fnResponse = new JSONObject();
                            JSONObject fnResponseContent = new JSONObject();
                            fnResponseContent.put("name", functionName);
                            JSONObject respObj = new JSONObject();
                            respObj.put("result", toolResult);
                            fnResponseContent.put("response", respObj);
                            fnResponse.put("functionResponse", fnResponseContent);
                            toolResponseParts.put(fnResponse);
                        }

                        // Thêm kết quả tool vào history
                        JSONObject toolTurn = new JSONObject();
                        toolTurn.put("role", "user");
                        toolTurn.put("parts", toolResponseParts);
                        conversationHistory.add(toolTurn);

                        // Tiếp tục vòng lặp để Gemini đọc kết quả tool và sinh câu trả lời
                        continue;
                    }

                    // Trường hợp AI trả về tin nhắn văn bản hoàn chỉnh
                    String finalReply = textBuffer.toString().trim();
                    if (finalReply.isEmpty() && !allSuggestedBooks.isEmpty()) {
                        finalReply = "Dưới đây là một số tựa sách phù hợp nhất tại nhà sách Thunder Book dành cho bạn:";
                    } else if (finalReply.isEmpty()) {
                        finalReply = "Xin lỗi bạn, tôi chưa tìm thấy thông tin sách phù hợp trong kho. Bạn hãy thử tìm với từ khóa hoặc thể loại khác nhé!";
                    }

                    // Lưu câu trả lời của model vào history
                    JSONObject finalModelTurn = new JSONObject();
                    finalModelTurn.put("role", "model");
                    JSONArray finalParts = new JSONArray();
                    JSONObject finalPart = new JSONObject();
                    finalPart.put("text", finalReply);
                    finalParts.put(finalPart);
                    finalModelTurn.put("parts", finalParts);
                    conversationHistory.add(finalModelTurn);

                    ChatMessage botMsg = new ChatMessage(finalReply, ChatMessage.TYPE_BOT, allSuggestedBooks);
                    mainHandler.post(() -> callback.onSuccess(botMsg));
                    return;
                }

                // Nếu chạy hết số lượt lặp mà chưa có text
                if (!allSuggestedBooks.isEmpty()) {
                    ChatMessage botMsg = new ChatMessage("Dưới đây là các cuốn sách phù hợp nhất tôi đã tìm thấy trong kho:", ChatMessage.TYPE_BOT, allSuggestedBooks);
                    mainHandler.post(() -> callback.onSuccess(botMsg));
                } else {
                    postError(callback, "AI mất nhiều thời gian xử lý. Vui lòng thử lại với câu hỏi ngắn gọn hơn.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi xử lý Chatbot: ", e);
                postError(callback, "Đã xảy ra lỗi khi kết nối với AI: " + e.getMessage());
            }
        });
    }

    /**
     * Xóa lịch sử phiên hội thoại hiện tại.
     */
    public void resetSession(String sessionId) {
        conversationHistory.clear();
    }

    private JSONObject buildGeminiPayload() throws Exception {
        JSONObject payload = new JSONObject();

        // Contents
        JSONArray contents = new JSONArray();
        for (JSONObject turn : conversationHistory) {
            contents.put(turn);
        }
        payload.put("contents", contents);

        // System Instruction
        JSONObject sysInstruction = new JSONObject();
        JSONArray sysParts = new JSONArray();
        JSONObject sysTextPart = new JSONObject();
        sysTextPart.put("text", SYSTEM_INSTRUCTION);
        sysParts.put(sysTextPart);
        sysInstruction.put("parts", sysParts);
        payload.put("system_instruction", sysInstruction);

        // Tools
        payload.put("tools", getToolsDeclaration());

        return payload;
    }

    private JSONObject callGeminiApi(JSONObject payload) throws IOException {
        RequestBody requestBody = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(Constants.GEMINI_API_URL)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                Log.e(TAG, "Gemini HTTP Error " + response.code() + ": " + err);
                return null;
            }
            if (response.body() == null) return null;
            return new JSONObject(response.body().string());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi gọi HTTP Gemini: ", e);
            return null;
        }
    }

    /**
     * Thực thi truy vấn dữ liệu sách trực tiếp từ Cloud Firestore.
     */
    private JSONObject executeFirestoreTool(String functionName, JSONObject args, List<Book> outSuggestedBooks) {
        JSONObject result = new JSONObject();
        try {
            if ("search_books".equals(functionName)) {
                String keyword = args.optString("keyword", "").trim();
                String genre = args.optString("genre", "").trim();
                double maxPrice = args.optDouble("max_price", -1);
                double minRating = args.optDouble("min_rating", -1);
                boolean onSale = args.optBoolean("on_sale", false);

                // Lấy sách từ Firestore (có timeout 10 giây)
                QuerySnapshot snapshot;
                try {
                    snapshot = Tasks.await(
                            FirebaseUtils.getFirestore()
                                    .collection(Constants.COLLECTION_BOOKS)
                                    .whereEqualTo(Constants.FIELD_IS_ACTIVE, true)
                                    .limit(80)
                                    .get(),
                            10, TimeUnit.SECONDS
                    );
                } catch (Exception e) {
                    // Fallback nếu query isActive gặp lỗi
                    snapshot = Tasks.await(
                            FirebaseUtils.getFirestore()
                                    .collection(Constants.COLLECTION_BOOKS)
                                    .limit(80)
                                    .get(),
                            10, TimeUnit.SECONDS
                    );
                }

                List<BookMatch> scoredBooks = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    Book book = doc.toObject(Book.class);
                    book.setBookId(doc.getId());

                    // Lọc theo giá tối đa
                    double effectivePrice = book.getSalePrice() > 0 ? book.getSalePrice() : book.getPrice();
                    if (maxPrice > 0 && effectivePrice > maxPrice) {
                        continue;
                    }

                    // Lọc theo điểm đánh giá tối thiểu
                    if (minRating > 0 && book.getRating() < minRating) {
                        continue;
                    }

                    // Lọc sách giảm giá
                    if (onSale && !book.isOnSale()) {
                        continue;
                    }

                    // Tính điểm khớp theo từ khóa & thể loại
                    double score = calculateMatchScore(book, keyword, genre);
                    if (score > 0) {
                        scoredBooks.add(new BookMatch(book, score));
                    }
                }

                // Sắp xếp sách theo điểm phù hợp nhất
                Collections.sort(scoredBooks, (a, b) -> Double.compare(b.score, a.score));

                JSONArray booksArray = new JSONArray();
                int limit = Math.min(5, scoredBooks.size());
                for (int i = 0; i < limit; i++) {
                    Book b = scoredBooks.get(i).book;
                    outSuggestedBooks.add(b);

                    JSONObject bObj = new JSONObject();
                    bObj.put("id", b.getBookId());
                    bObj.put("title", b.getTitle());
                    bObj.put("price", b.getPrice());
                    bObj.put("salePrice", b.getSalePrice());
                    bObj.put("stock", b.getStock());
                    bObj.put("rating", b.getRating());
                    bObj.put("soldCount", b.getSoldCount());
                    bObj.put("description", b.getDescription());
                    bObj.put("coverImageUrl", b.getCoverImageUrl());
                    booksArray.put(bObj);
                }

                result.put("total_found", scoredBooks.size());
                result.put("books", booksArray);

            } else if ("check_stock".equals(functionName)) {
                String bookId = args.optString("book_id", "");
                if (bookId.isEmpty()) {
                    result.put("error", "Vui lòng cung cấp mã book_id.");
                    return result;
                }

                DocumentSnapshot doc = Tasks.await(
                        FirebaseUtils.getFirestore()
                                .collection(Constants.COLLECTION_BOOKS)
                                .document(bookId)
                                .get(),
                        10, TimeUnit.SECONDS
                );

                if (doc.exists()) {
                    Book book = doc.toObject(Book.class);
                    if (book != null) {
                        book.setBookId(doc.getId());
                        outSuggestedBooks.add(book);

                        result.put("id", book.getBookId());
                        result.put("title", book.getTitle());
                        result.put("stock", book.getStock());
                        result.put("status", book.getStock() > 0 ? "Còn hàng" : "Tạm hết hàng");
                        result.put("price", book.getPrice());
                        result.put("salePrice", book.getSalePrice());
                    }
                } else {
                    result.put("error", "Không tìm thấy sách với mã " + bookId);
                }

            } else if ("get_book_detail".equals(functionName)) {
                String bookId = args.optString("book_id", "");
                DocumentSnapshot doc = Tasks.await(
                        FirebaseUtils.getFirestore()
                                .collection(Constants.COLLECTION_BOOKS)
                                .document(bookId)
                                .get(),
                        10, TimeUnit.SECONDS
                );

                if (doc.exists()) {
                    Book book = doc.toObject(Book.class);
                    if (book != null) {
                        book.setBookId(doc.getId());
                        outSuggestedBooks.add(book);

                        result.put("id", book.getBookId());
                        result.put("title", book.getTitle());
                        result.put("price", book.getPrice());
                        result.put("salePrice", book.getSalePrice());
                        result.put("stock", book.getStock());
                        result.put("rating", book.getRating());
                        result.put("description", book.getDescription());
                    }
                } else {
                    result.put("error", "Không tìm thấy sách với mã " + bookId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi truy vấn Firestore trong Tool: ", e);
            try {
                result.put("error", "Lỗi truy vấn Firestore: " + e.getMessage());
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * Thuật toán tính điểm khớp từ khóa tìm kiếm sách thông minh (hỗ trợ tiếng Việt không dấu & có dấu).
     */
    private double calculateMatchScore(Book book, String keyword, String genre) {
        if ((keyword == null || keyword.isEmpty()) && (genre == null || genre.isEmpty())) {
            return 1.0 + (book.getRating() * 2) + (book.getSoldCount() * 0.05);
        }

        String normTitle = stripAccents(book.getTitle() != null ? book.getTitle() : "");
        String normDesc = stripAccents(book.getDescription() != null ? book.getDescription() : "");
        
        StringBuilder tagsBuilder = new StringBuilder();
        if (book.getTags() != null) {
            for (String t : book.getTags()) {
                tagsBuilder.append(t).append(" ");
            }
        }
        String normTags = stripAccents(tagsBuilder.toString());

        double score = 0.0;

        // 1. Khớp nguyên cụm từ khóa (Phrase match)
        if (keyword != null && !keyword.isEmpty()) {
            String normKeyword = stripAccents(keyword);
            if (normTitle.contains(normKeyword)) {
                score += 60.0;
            }
            if (normTags.contains(normKeyword)) {
                score += 35.0;
            }
            if (normDesc.contains(normKeyword)) {
                score += 25.0;
            }

            // 2. Khớp từng từ đơn (Token match)
            String[] tokens = normKeyword.split("\\s+");
            for (String t : tokens) {
                if (t.length() <= 1) continue;
                // Bỏ qua các hư từ / từ dừng phổ biến
                if (isStopWord(t)) continue;

                if (normTitle.contains(t)) {
                    score += 15.0;
                }
                if (normTags.contains(t)) {
                    score += 10.0;
                }
                if (normDesc.contains(t)) {
                    score += 4.0;
                }
            }
        }

        // 3. Khớp thể loại (Genre match)
        if (genre != null && !genre.isEmpty()) {
            String normGenre = stripAccents(genre);
            if (normDesc.contains(normGenre) || normTitle.contains(normGenre) || normTags.contains(normGenre)) {
                score += 30.0;
            }
        }

        // 4. Cộng điểm uy tín từ đánh giá & số lượng đã bán
        if (score > 0) {
            score += (book.getRating() * 2.0) + (book.getSoldCount() * 0.05);
        }

        return score;
    }

    private boolean isStopWord(String word) {
        return "va".equals(word) || "cac".equals(word) || "nhung".equals(word) || "cho".equals(word)
                || "toi".equals(word) || "hay".equals(word) || "giup".equals(word) || "bot".equals(word)
                || "cuon".equals(word) || "sach".equals(word) || "nhieu".equals(word) || "ve".equals(word)
                || "la".equals(word) || "mot".equals(word) || "trong".equals(word) || "muon".equals(word)
                || "tim".equals(word) || "co".equals(word);
    }

    private String stripAccents(String s) {
        if (s == null || s.isEmpty()) return "";
        String nfkd = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfkd).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase()
                .trim();
    }

    private static class BookMatch {
        final Book book;
        final double score;

        BookMatch(Book book, double score) {
            this.book = book;
            this.score = score;
        }
    }

    private JSONArray getToolsDeclaration() throws Exception {
        JSONArray tools = new JSONArray();
        JSONObject toolObj = new JSONObject();
        JSONArray fnDeclarations = new JSONArray();

        // 1. search_books
        JSONObject searchFn = new JSONObject();
        searchFn.put("name", "search_books");
        searchFn.put("description", "Tìm kiếm và tra cứu sách trong kho theo từ khóa (tên sách, chủ đề), thể loại, mức giá tối đa, điểm đánh giá hoặc giảm giá.");

        JSONObject searchParams = new JSONObject();
        searchParams.put("type", "object");
        JSONObject searchProps = new JSONObject();

        JSONObject kwProp = new JSONObject();
        kwProp.put("type", "string");
        kwProp.put("description", "Tên sách, tác giả hoặc từ khóa chủ đề (ví dụ: thói quen, tâm lý, chữa lành, kinh tế)");
        searchProps.put("keyword", kwProp);

        JSONObject genreProp = new JSONObject();
        genreProp.put("type", "string");
        genreProp.put("description", "Thể loại sách cụ thể");
        searchProps.put("genre", genreProp);

        JSONObject priceProp = new JSONObject();
        priceProp.put("type", "number");
        priceProp.put("description", "Mức giá tối đa mong muốn (VND)");
        searchProps.put("max_price", priceProp);

        JSONObject ratingProp = new JSONObject();
        ratingProp.put("type", "number");
        ratingProp.put("description", "Điểm đánh giá tối thiểu (1.0 - 5.0)");
        searchProps.put("min_rating", ratingProp);

        JSONObject saleProp = new JSONObject();
        saleProp.put("type", "boolean");
        saleProp.put("description", "Lọc các sách đang có giảm giá");
        searchProps.put("on_sale", saleProp);

        searchParams.put("properties", searchProps);
        searchFn.put("parameters", searchParams);
        fnDeclarations.put(searchFn);

        // 2. check_stock
        JSONObject stockFn = new JSONObject();
        stockFn.put("name", "check_stock");
        stockFn.put("description", "Kiểm tra số lượng tồn kho và giá bán hiện tại của sách.");
        JSONObject stockParams = new JSONObject();
        stockParams.put("type", "object");
        JSONObject stockProps = new JSONObject();
        JSONObject idProp = new JSONObject();
        idProp.put("type", "string");
        idProp.put("description", "Mã book_id của sách");
        stockProps.put("book_id", idProp);
        stockParams.put("properties", stockProps);
        JSONArray requiredStock = new JSONArray();
        requiredStock.put("book_id");
        stockParams.put("required", requiredStock);
        stockFn.put("parameters", stockParams);
        fnDeclarations.put(stockFn);

        // 3. get_book_detail
        JSONObject detailFn = new JSONObject();
        detailFn.put("name", "get_book_detail");
        detailFn.put("description", "Lấy toàn bộ thông tin chi tiết và mô tả của sách theo book_id.");
        JSONObject detailParams = new JSONObject();
        detailParams.put("type", "object");
        JSONObject detailProps = new JSONObject();
        JSONObject dIdProp = new JSONObject();
        dIdProp.put("type", "string");
        dIdProp.put("description", "Mã book_id của sách");
        detailProps.put("book_id", dIdProp);
        detailParams.put("properties", detailProps);
        JSONArray requiredDetail = new JSONArray();
        requiredDetail.put("book_id");
        detailParams.put("required", requiredDetail);
        detailFn.put("parameters", detailParams);
        fnDeclarations.put(detailFn);

        toolObj.put("functionDeclarations", fnDeclarations);
        tools.put(toolObj);
        return tools;
    }

    private void postError(ChatCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
