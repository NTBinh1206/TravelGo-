package com.travelgo.app.data.place

/**
 * Các luật chống báo cáo giả, viết thành hàm thuần (không dùng Firebase) để unit test.
 * Luật Firestore (firestore.rules) kiểm tra lại y hệt trên máy chủ, nên sửa app cũng không vượt qua được.
 */
object ReportGuards {
    private const val HOUR_MS = 60 * 60 * 1000L

    /** Tài khoản Google phải dùng TravelGo ít nhất ngần này mới được báo hoặc xác nhận quán. */
    const val MIN_ACCOUNT_AGE_MS = 24 * HOUR_MS

    /** Mỗi tài khoản báo + xác nhận tối đa [DAILY_LIMIT] lần trong [WINDOW_MS], tính từ lần đầu trong khung. */
    const val DAILY_LIMIT = 5
    const val WINDOW_MS = 24 * HOUR_MS

    /** Phải đứng cách quán không quá ngần này (mét) mới được báo hoặc xác nhận. */
    const val NEAR_METERS = 200.0

    /** Quán ít người xem: cần [MIN_REPORTS] tài khoản báo; cứ thêm [VIEWERS_PER_EXTRA] người xem thì cần thêm 1, tối đa [MAX_REPORTS]. */
    const val MIN_REPORTS = 2
    const val VIEWERS_PER_EXTRA = 10
    const val MAX_REPORTS = 5

    const val TOO_NEW = "Tài khoản cần dùng TravelGo ít nhất 1 ngày mới được báo hoặc xác nhận quán. Bạn thử lại sau nhé."
    const val LIMIT_REACHED = "Mỗi tài khoản chỉ báo hoặc xác nhận được $DAILY_LIMIT quán trong 24 giờ. Bạn thử lại sau nhé."

    fun oldEnough(createdAtMs: Long, nowMs: Long): Boolean = nowMs - createdAtMs >= MIN_ACCOUNT_AGE_MS

    /** Lần làm này còn nằm trong khung 24 giờ cũ không; quá rồi thì mở khung mới. */
    fun inWindow(windowStartMs: Long, nowMs: Long): Boolean = nowMs < windowStartMs + WINDOW_MS

    /** Số lần đã dùng sau khi làm thêm một lần; null nếu đã hết lượt trong khung hiện tại. */
    fun nextCount(windowStartMs: Long, used: Int, nowMs: Long): Int? =
        if (!inWindow(windowStartMs, nowMs)) 1 else (used + 1).takeIf { it <= DAILY_LIMIT }

    /** Số tài khoản thật cần báo để gắn nhãn, theo số người (tài khoản thật) đã xem quán. */
    fun requiredReports(viewers: Int): Int =
        (MIN_REPORTS + viewers / VIEWERS_PER_EXTRA).coerceAtMost(MAX_REPORTS)
}
