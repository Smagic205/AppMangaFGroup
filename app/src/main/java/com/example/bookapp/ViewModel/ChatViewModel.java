package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.ChatMessage;
import com.example.bookapp.Repository.ChatRepository;
import com.example.bookapp.Utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ViewModel quản lý trạng thái hội thoại và logic gửi/nhận tin nhắn với Chatbot AI.
 */
public class ChatViewModel extends ViewModel {

    private final ChatRepository repository;
    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isTypingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    private String sessionId;

    public ChatViewModel() {
        this.repository = new ChatRepository();
        this.sessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
        initWelcomeMessage();
    }

    private void initWelcomeMessage() {
        List<ChatMessage> list = new ArrayList<>();
        ChatMessage welcomeMsg = new ChatMessage(
                "Xin chào! Tôi là Trợ lý Tư vấn Sách AI của nhà sách Thunder Book ⚡📚\n\n" +
                "Tôi có thể giúp bạn tìm sách theo sở thích, tâm trạng, gợi ý sách bán chạy, kiểm tra giá ưu đãi và tồn kho. Bạn đang muốn tìm cuốn sách như thế nào?",
                ChatMessage.TYPE_BOT
        );
        list.add(welcomeMsg);
        messagesLiveData.setValue(list);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }

    public LiveData<Boolean> getIsTyping() {
        return isTypingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public void sendMessage(String userText) {
        if (userText == null || userText.trim().isEmpty()) return;

        String cleanText = userText.trim();
        List<ChatMessage> currentList = messagesLiveData.getValue();
        if (currentList == null) currentList = new ArrayList<>();

        // 1. Thêm tin nhắn của User
        currentList.add(new ChatMessage(cleanText, ChatMessage.TYPE_USER));

        // 2. Thêm tin nhắn Loading Typing của Bot
        ChatMessage loadingMessage = ChatMessage.createLoadingMessage();
        currentList.add(loadingMessage);
        messagesLiveData.setValue(new ArrayList<>(currentList));
        isTypingLiveData.setValue(true);

        String uid = FirebaseUtils.getCurrentUserId();

        // 3. Gửi lên Chatbot Server
        repository.sendMessage(uid, sessionId, cleanText, new ChatRepository.ChatCallback() {
            @Override
            public void onSuccess(ChatMessage botResponse) {
                List<ChatMessage> list = messagesLiveData.getValue();
                if (list != null && !list.isEmpty()) {
                    // Xóa tin nhắn loading cuối cùng
                    if (list.get(list.size() - 1).isLoading()) {
                        list.remove(list.size() - 1);
                    }
                    // Thêm tin nhắn bot trả về
                    list.add(botResponse);
                    messagesLiveData.setValue(new ArrayList<>(list));
                }
                isTypingLiveData.setValue(false);
            }

            @Override
            public void onError(String errorMessage) {
                List<ChatMessage> list = messagesLiveData.getValue();
                if (list != null && !list.isEmpty()) {
                    // Xóa tin nhắn loading cuối cùng
                    if (list.get(list.size() - 1).isLoading()) {
                        list.remove(list.size() - 1);
                    }
                    // Thêm thông báo lỗi
                    ChatMessage errorBotMsg = new ChatMessage(
                            "⚠️ " + errorMessage,
                            ChatMessage.TYPE_BOT
                    );
                    list.add(errorBotMsg);
                    messagesLiveData.setValue(new ArrayList<>(list));
                }
                isTypingLiveData.setValue(false);
                errorMessageLiveData.setValue(errorMessage);
            }
        });
    }

    public void clearHistory() {
        repository.resetSession(sessionId);
        this.sessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
        initWelcomeMessage();
    }
}
