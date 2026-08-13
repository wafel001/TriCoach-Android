package pl.tricoach.mobile;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class JobSchedulerHelper {
    private static final int JOB_ID = 2202;
    private JobSchedulerHelper() {}

    public static void schedule(Context c) {
        Store store = new Store(c);
        if (!store.autoSync()) return;
        JobScheduler js = c.getSystemService(JobScheduler.class);
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, SyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000L)
                .build();
        js.schedule(info);
    }
}
