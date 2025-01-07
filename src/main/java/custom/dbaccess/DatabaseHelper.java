package custom.dbaccess;

import ca.uhn.fhir.batch2.model.JobInstance;
import org.quartz.Job;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
	public static JobInstance GetJobInstanceByJobId(String jobId, String tenantname) {
		JobInstance jobInstance = new JobInstance();

		String query = "SELECT id, job_cancelled, cmb_recs_processed, cmb_recs_per_sec, create_time, cur_gated_step_id, definition_id, definition_ver, end_time, error_count, error_msg, est_remaining, fast_tracking, params_json, params_json_lob, params_json_vc, progress_pct, report, report_vc, start_time, stat, tot_elapsed_millis, client_id, user_name, update_time, warning_msg, work_chunks_purged FROM BT2_JOB_INSTANCE WHERE ID = ?";
		Object[] params = new Object[] { jobId };
		DataLayer dblayer = new DataLayer(tenantname);

		try {
			dblayer.Connect();
			List<Map<String, Object>> result = dblayer.execute(query, params);
			for (Map<String, Object> row : result) {

				jobInstance.setCancelled((Boolean) row.get("job_cancelled"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally {
			try{
				dblayer.CloseConnection();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return jobInstance;
	}

}
