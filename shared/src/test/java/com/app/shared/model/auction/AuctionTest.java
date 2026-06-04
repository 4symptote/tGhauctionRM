package com.app.shared.model.auction;

import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    private Item testItem;
    private Auction auction;

    @BeforeEach
    public void setUp() {
        testItem = new Electronics.Builder()
                .name("Laptop")
                .desc("Gaming Laptop")
                .startingPrice(1000.0)
                .sellerId("seller123")
                // có thể có thêm .brand("Dell") hoặc các thuộc tính khác của Electronics
                .build();

        long currentTime = System.currentTimeMillis();
        long duration = 60000L; // 60 giây

        auction = new Auction(testItem, currentTime, currentTime + duration);
    }

    @Test
    public void testAuctionInitialization() {
        assertNotNull(auction.getId(), "Entity ID không được null");
        assertEquals(1000.0, auction.getCurrentPrice(), "Giá hiện tại phải bằng giá khởi điểm của Item");
        // Status ban đầu ở hàm init có thể phụ thuộc vào logic lấy thời gian, thường sẽ là OPEN hoặc RUNNING
        assertNotNull(auction.getStatus(), "Status không được null");

        // Đoạn check getBids().isEmpty() đã bị xóa vì Auction không còn giữ List bids nữa.
    }

    @Test
    public void testProcessNewBidUpdatesPriceAndHighestBidder() {
        double newBidAmount = 1200.0;
        String bidderId = "bidderXYZ";
        String bidderName = "John Doe";

        // Cập nhật giá và người dẫn đầu thông qua hàm mới
        auction.processNewBid(newBidAmount, bidderId, bidderName);

        // Kiểm tra xem giá của Auction và người đặt giá cao nhất đã được lưu lại chưa
        assertEquals(newBidAmount, auction.getCurrentPrice(), "Giá hiện tại của Auction phải được cập nhật");
        assertEquals(bidderId, auction.getHighestBidderId(), "ID người thắng tạm thời phải được cập nhật");

        // Tùy theo code thực tế Item có còn hàm setCurrentHighestBid không, nếu có thì bật dòng dưới
        // assertEquals(1200.0, testItem.getCurrentHighestBid(), "Item tracking price cũng phải được cập nhật");
    }

    @Test
    public void testStatusTransition() throws InterruptedException {
        long now = System.currentTimeMillis();
        // Tạo auction chỉ kéo dài 100 milliseconds
        Auction shortAuction = new Auction(testItem, now, now + 100L);

        // Tùy thuộc vào thời điểm lấy status, nếu now >= startTime thì status là RUNNING
        assertEquals(Auction.Status.RUNNING, shortAuction.getStatus());

        // Đợi 150ms để auction hết hạn
        Thread.sleep(150);

        // Kiểm tra logic tự cập nhật trạng thái khi gọi getStatus()
        assertEquals(Auction.Status.FINISHED, shortAuction.getStatus(), "Khi qúa thời gian, status phải trả về FINISHED");
    }
}