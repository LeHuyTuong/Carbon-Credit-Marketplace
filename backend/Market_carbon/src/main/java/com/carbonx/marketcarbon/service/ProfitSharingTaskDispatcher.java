package com.carbonx.marketcarbon.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Chuyển việc chạy payout sang pool profit-sharing sau khi request thread đã validate
 * và claim distribution xong.
 *
 * <p>Không dùng {@code @Async} cho bước retry vì retry cần trả lỗi validate (400/403/404)
 * ngay cho client. Tách dispatcher thành bean riêng để tránh self-invocation của proxy
 * Spring và để test có thể thay bằng executor chạy đồng bộ.</p>
 */
@Component
public class ProfitSharingTaskDispatcher {

    private final TaskExecutor executor;

    public ProfitSharingTaskDispatcher(
            @Qualifier("profitSharingTaskExecutor") TaskExecutor executor) {
        this.executor = executor;
    }

    public void submit(Runnable task) {
        executor.execute(task);
    }
}
