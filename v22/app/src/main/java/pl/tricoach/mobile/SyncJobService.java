package pl.tricoach.mobile;

import android.app.job.JobParameters;
import android.app.job.JobService;
import org.json.JSONObject;

public final class SyncJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                SecretStore secrets = new SecretStore(this);
                if (secrets.has()) {
                    Store store = new Store(this);
                    JSONObject raw = new IntervalsClient(secrets.get()).snapshot();
                    JSONObject dash = DashboardBuilder.build(raw, store.language());
                    store.saveDashboard(dash);
                }
            } catch (Exception ignored) {
            } finally {
                jobFinished(params, false);
            }
        }, "tricoach-sync").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
