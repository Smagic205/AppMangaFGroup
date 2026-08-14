package com.example.bookapp.Utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DataSeeder {
/**
    public static void seedSampleData(FirebaseCallback<Void> callback) {
        FirebaseFirestore db = FirebaseUtils.getFirestore();
        WriteBatch batch = db.batch();

        // ===== 1. CATEGORIES =====
        CollectionReference categoriesRef = db.collection("categories");
        DocumentReference catVanHoc = categoriesRef.document();
        DocumentReference catKyNang = categoriesRef.document();
        DocumentReference catKinhTe = categoriesRef.document();

        // THAY LINK SUPABASE VÀO ĐÂY
        batch.set(catVanHoc, categoryData("Văn học", "https://www.google.com/imgres?q=kinh%20t%E1%BA%BF%20icon&imgurl=https%3A%2F%2Fcdn-icons-png.magnific.com%2F256%2F7665%2F7665588.png%3Fsemt%3Dais_white_label&imgrefurl=https%3A%2F%2Fwww.magnific.com%2Fvn%2Ficons%2Fkinh-te&docid=bpBU730bdFUyMM&tbnid=1FS9b-VII4iu-M&vet=12ahUKEwib3d38lqCWAxX8gK8BHdAYNdsQnPAOegQIPBAA..i&w=256&h=256&hcb=2&ved=2ahUKEwib3d38lqCWAxX8gK8BHdAYNdsQnPAOegQIPBAA"));
        batch.set(catKyNang, categoryData("Kỹ năng sống", "https://www.google.com/imgres?q=kinh%20t%E1%BA%BF%20icon&imgurl=https%3A%2F%2Fpng.pngtree.com%2Fpng-vector%2F20190601%2Fourmid%2Fpngtree-financial-economic-rise-curve-png-image_1144566.jpg&imgrefurl=https%3A%2F%2Fvi.pngtree.com%2Ffree-png-vectors%2Fkinh-t%25E1%25BA%25BF-icon&docid=saNBL2-tsJQutM&tbnid=9PPyW9u2AhzeAM&vet=12ahUKEwib3d38lqCWAxX8gK8BHdAYNdsQnPAOegQIOBAA..i&w=360&h=360&hcb=2&ved=2ahUKEwib3d38lqCWAxX8gK8BHdAYNdsQnPAOegQIOBAA"));
        batch.set(catKinhTe, categoryData("Kinh tế", "https://www.google.com/imgres?q=kinh%20t%E1%BA%BF%20icon&imgurl=https%3A%2F%2Fpng.pngtree.com%2Ftemplate%2F20191014%2Fourmid%2Fpngtree-new-age-economy-finance-logo-logotype-corporation-icon-image_318279.jpg&imgrefurl=https%3A%2F%2Fvi.pngtree.com%2Fso%2Fbi%25E1%25BB%2583u-t%25C6%25B0%25E1%25BB%25A3ng-kinh-t%25E1%25BA%25BF&docid=UTNWcDzKD_I1eM&tbnid=UGN5cA2QDHUp9M&vet=12ahUKEwib3d38lqCWAxX8gK8BHdAYNdsQnPAOegUIhgEQAA..i&w=360&h=360&hcb=2&ved=2ahUKEwib3d38lqCWAxX8gK8BHdAYNdsQnPAOegUIhgEQAA"));

        // ===== 2. AUTHORS =====
        CollectionReference authorsRef = db.collection("authors");
        DocumentReference authorPaulo = authorsRef.document();
        DocumentReference authorDale = authorsRef.document();
        DocumentReference authorNapoleon = authorsRef.document();

        // Bạn cũng có thể thêm avatarUrl cho tác giả từ Supabase ở đây nếu muốn sửa hàm authorData
        batch.set(authorPaulo, authorData("Paulo Coelho", "Nhà văn người Brazil, tác giả Nhà Giả Kim."));
        batch.set(authorDale, authorData("Dale Carnegie", "Tác giả sách kỹ năng sống nổi tiếng người Mỹ."));
        batch.set(authorNapoleon, authorData("Napoleon Hill", "Tác giả sách phát triển bản thân, tài chính."));

        // ===== 3. PUBLISHERS =====
        CollectionReference publishersRef = db.collection("publishers");
        DocumentReference pubNhaNam = publishersRef.document();
        DocumentReference pubTre = publishersRef.document();

        batch.set(pubNhaNam, publisherData("Nhã Nam", "Đơn vị xuất bản sách văn học, kỹ năng hàng đầu Việt Nam."));
        batch.set(pubTre, publisherData("NXB Trẻ", "Nhà xuất bản sách trẻ, đa dạng thể loại."));

        // ===== 4. BOOKS =====
        CollectionReference booksRef = db.collection("books");

        batch.set(booksRef.document(), bookData(
                "Nhà Giả Kim", "nha-gia-kim",
                Arrays.asList(authorPaulo.getId()), pubNhaNam.getId(),
                Arrays.asList(catVanHoc.getId()),
                "Câu chuyện về hành trình đi tìm kho báu và ý nghĩa cuộc sống của chàng chăn cừu Santiago.",
                "https://lgwiftzrrebphjymvbbc.supabase.co/storage/v1/object/public/cover-book/Nha-gia-kim.jpg", // <--- THAY LINK ẢNH BÌA SÁCH TỪ SUPABASE
                89000, 79000, 50, 2020, Arrays.asList("bestseller"), true));

        batch.set(booksRef.document(), bookData(
                "Đắc Nhân Tâm", "dac-nhan-tam",
                Arrays.asList(authorDale.getId()), pubTre.getId(),
                Arrays.asList(catKyNang.getId()),
                "Cuốn sách kinh điển về nghệ thuật đối nhân xử thế, được dịch ra hàng chục thứ tiếng.",
                "https://lgwiftzrrebphjymvbbc.supabase.co/storage/v1/object/public/cover-book/dac_nhan_tam.webp", // <--- THAY LINK ẢNH BÌA SÁCH TỪ SUPABASE
                108000, 88000, 40, 2019, Arrays.asList("bestseller", "sale"), true));

        batch.set(booksRef.document(), bookData(
                "Nghĩ Giàu Làm Giàu", "nghi-giau-lam-giau",
                Arrays.asList(authorNapoleon.getId()), pubTre.getId(),
                Arrays.asList(catKinhTe.getId()),
                "13 nguyên tắc thành công được đúc kết từ nghiên cứu hơn 500 người thành đạt.",
                "https://lgwiftzrrebphjymvbbc.supabase.co/storage/v1/object/public/cover-book/nghi_giau_lam_giau.webp", // <--- THAY LINK ẢNH BÌA SÁCH TỪ SUPABASE
                120000, 99000, 30, 2021, Arrays.asList("new"), false));

        // ===== COMMIT TẤT CẢ CÙNG LÚC =====
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    // Các hàm helper bên dưới (categoryData, authorData, publisherData, bookData) GIỮ NGUYÊN không cần sửa gì cả.
    private static Map<String, Object> categoryData(String name, String imageUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("imageUrl", imageUrl);
        map.put("isActive", true);
        return map;
    }

    private static Map<String, Object> authorData(String name, String bio) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("avatarUrl", "");
        map.put("bio", bio);
        map.put("bookCount", 1);
        return map;
    }

    private static Map<String, Object> publisherData(String name, String description) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("logoUrl", "");
        map.put("description", description);
        return map;
    }

    private static Map<String, Object> bookData(
            String title, String slug, java.util.List<String> authorIds, String publisherId,
            java.util.List<String> categoryIds, String description, String coverImageUrl,
            double price, double salePrice, int stock, int publishYear,
            java.util.List<String> tags, boolean isFeatured) {

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("slug", slug);
        map.put("authorIds", authorIds);
        map.put("publisherId", publisherId);
        map.put("categoryIds", categoryIds);
        map.put("description", description);
        map.put("coverImageUrl", coverImageUrl);
        map.put("price", price);
        map.put("salePrice", salePrice);
        map.put("stock", stock);
        map.put("soldCount", 0);
        map.put("viewCount", 0);
        map.put("rating", 0);
        map.put("ratingCount", 0);
        map.put("publishYear", publishYear);
        map.put("tags", tags);
        map.put("isFeatured", isFeatured);
        map.put("isActive", true);
        return map;
    }

 **/
}