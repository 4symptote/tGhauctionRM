package com.app.shared.model.item.factory;

import com.app.shared.model.item.Art;
import com.app.shared.model.item.Item;
import com.app.shared.network.payload.CreateAuctionPayload;

import java.util.Map;

public class ArtCreator implements ItemCreator {

    @Override
    public Item createItem(CreateAuctionPayload payload) {

        //Extract custom attributes from the payload - Làm sau
        Map<String, Object> attrs = payload.customAttributes();
        if (attrs == null) {
            attrs = Map.of();
        }

        // dùng Builder - xem Art
        return new Art.Builder() // cách dòng nhìn cho đẹp
                .name(payload.name())
                .desc(payload.description())
                .sellerId(payload.sellerId())
                .startingPrice(payload.startingPrice())

                .artist((String) attrs.getOrDefault("artist", "Unknown"))
                .medium((String) attrs.getOrDefault("medium", "Unknown"))
                .year((int) attrs.getOrDefault("year", 0))

                .build();
        /*
        Thay vì phải căng mắt viết một hàm khởi tạo khổng lồ và dễ truyền nhầm vị trí
        như `new Art("Tên", "Mô tả", 500, "user1", "Da Vinci", "Sơn dầu", 1503, .....)`,
         Builder giúp việc gán giá trị trở nên tường minh qua từng bước
        các giá trị custom có thể có hoặc không -> more linh hoạt
        vd. có thể ng dùng ko nhập năm (1503 như trên), thuộc tính sẽ được bỏ qua hoặc gán giá trị mặc đinh (qua payload)

        Và vì nhìn rất đẹp.

        Ngoài ra, nhúng trực tiếp Payload vào vì
        cách truyền thống: `createItem(name, price, artist, year)`, khi thêm loại
        tài sản mới như Bất động sản (cần diện tích, số phòng...), ta sẽ phải sửa lại toàn bộ
        interface và các class cũ. Bằng cách truyền Payload chứa `Map<String, Object>`,
        interface `ItemCreator` luôn cố định: `createItem(CreateAuctionPayload payload)`.
        Hệ thống có thể mở rộng vô hạn mà không làm vỡ kiến trúc cũ.

        Và Việc "bóc hộp" dữ liệu được giao cho đúng người cần nó. Chỉ có `ArtCreator` mới tự động
        chọc vào `customAttributes` để lấy ra `artist` và `year`. Các thành phần khác của
        server hoàn toàn mù tịt và không cần bận tâm về những dữ liệu riêng biệt này.
        Server không cần check xem item này là gì rồi set từng attribute riêng của các type.

        Repo cũ thậm chí còn đéo dùng mấy cái custom attr đấy.
        */
    }
}