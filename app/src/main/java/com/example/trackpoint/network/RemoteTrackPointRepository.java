package com.example.trackpoint.network;

import android.content.Context;
import android.net.Uri;
import com.example.trackpoint.S3StorageManager;
import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.DepartmentDto;
import com.example.trackpoint.network.dto.StaffAdminDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.network.dto.SuperAdminDto;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemoteTrackPointRepository {

    private final TrackPointApiService apiService;
    private final S3StorageManager s3Manager;

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public RemoteTrackPointRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
        this.s3Manager = S3StorageManager.getInstance(context);
    }

    // --- DAILY ACTIVITIES (Neon PostgreSQL + Backblaze B2 S3) ---

    /**
     * Uploads activity file to Backblaze B2 S3 bucket, then registers file_key and metadata into Neon PostgreSQL.
     */
    public void submitDailyActivity(Context context, Uri fileUri, String studentRegNo, String dateKey, String notes, String submitTime, RepositoryCallback<DailyActivityDto> callback) {
        if (fileUri == null) {
            DailyActivityDto dto = new DailyActivityDto(studentRegNo, dateKey, "", "", "", notes, submitTime);
            saveToNeon(dto, callback);
            return;
        }

        // 1. Upload to Backblaze B2 via S3StorageManager
        s3Manager.uploadFile(context, fileUri, studentRegNo, dateKey, new S3StorageManager.S3UploadCallback() {
            @Override
            public void onSuccess(String objectKey, String presignedUrl) {
                String fileName = objectKey.contains("_") ? objectKey.substring(objectKey.indexOf("_") + 1) : "upload_file";

                // 2. Prepare DTO containing B2 S3 object key + presigned URL
                DailyActivityDto dto = new DailyActivityDto(
                        studentRegNo,
                        dateKey,
                        fileName,
                        objectKey,     // Saved to file_key column in Neon
                        presignedUrl,  // Presigned URL
                        notes,
                        submitTime
                );

                // 3. Save Record into Neon PostgreSQL Database via Retrofit
                saveToNeon(dto, callback);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    private void saveToNeon(DailyActivityDto dto, RepositoryCallback<DailyActivityDto> callback) {
        apiService.upsertDailyActivity(dto).enqueue(new Callback<DailyActivityDto>() {
            @Override
            public void onResponse(Call<DailyActivityDto> call, Response<DailyActivityDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    /**
     * Fetches daily activity from Neon and refreshes the presigned B2 URL dynamically.
     */
    public void getDailyActivity(String studentRegNo, String dateKey, RepositoryCallback<DailyActivityDto> callback) {
        apiService.getDailyActivity(studentRegNo, dateKey).enqueue(new Callback<DailyActivityDto>() {
            @Override
            public void onResponse(Call<DailyActivityDto> call, Response<DailyActivityDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DailyActivityDto activity = response.body();

                    // Dynamically generate fresh 1-hour presigned URL from Backblaze B2 using stored file_key
                    if (activity.getFileKey() != null && !activity.getFileKey().isEmpty()) {
                        String freshPresignedUrl = s3Manager.generatePresignedUrl(activity.getFileKey(), 60);
                        activity.setFileUrl(freshPresignedUrl);
                    }

                    if (callback != null) callback.onSuccess(activity);
                } else {
                    if (callback != null) callback.onError(new Exception("Activity not found"));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    /**
     * Deletes daily activity from Neon database and purges file from Backblaze B2.
     */
    public void deleteDailyActivity(String studentRegNo, String dateKey, String fileKey, RepositoryCallback<Void> callback) {
        // 1. Delete file from Backblaze B2 if fileKey exists
        if (fileKey != null && !fileKey.isEmpty()) {
            s3Manager.deleteFile(fileKey, new S3StorageManager.S3DeleteCallback() {
                @Override
                public void onSuccess() {
                    deleteFromNeon(studentRegNo, dateKey, callback);
                }

                @Override
                public void onError(Exception e) {
                    // Proceed with database deletion even if S3 delete fails
                    deleteFromNeon(studentRegNo, dateKey, callback);
                }
            });
        } else {
            deleteFromNeon(studentRegNo, dateKey, callback);
        }
    }

    private void deleteFromNeon(String studentRegNo, String dateKey, RepositoryCallback<Void> callback) {
        apiService.deleteDailyActivity(studentRegNo, dateKey).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess(null);
                } else {
                    if (callback != null) callback.onError(new Exception("Delete failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void updateStaffRemark(String studentRegNo, String dateKey, String remark, RepositoryCallback<DailyActivityDto> callback) {
        apiService.updateStaffRemark(studentRegNo, dateKey, remark).enqueue(new Callback<DailyActivityDto>() {
            @Override
            public void onResponse(Call<DailyActivityDto> call, Response<DailyActivityDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Update remark failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- ATTENDANCE (Neon PostgreSQL) ---

    public void recordAttendance(String studentRegNo, String dateKey, String status, String markedBy, RepositoryCallback<AttendanceDto> callback) {
        AttendanceDto dto = new AttendanceDto(studentRegNo, dateKey, status, markedBy);
        apiService.recordAttendance(dto).enqueue(new Callback<AttendanceDto>() {
            @Override
            public void onResponse(Call<AttendanceDto> call, Response<AttendanceDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Attendance record failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<AttendanceDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void recordAttendanceBatch(List<AttendanceDto> records, RepositoryCallback<Void> callback) {
        apiService.recordAttendanceBatch(records).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess(null);
                } else {
                    if (callback != null) callback.onError(new Exception("Batch attendance failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- STUDENTS (Neon PostgreSQL) ---

    public void getAllStudents(RepositoryCallback<List<StudentDto>> callback) {
        apiService.getAllStudents().enqueue(new Callback<List<StudentDto>>() {
            @Override
            public void onResponse(Call<List<StudentDto>> call, Response<List<StudentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch students failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<StudentDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void createStudent(StudentDto student, RepositoryCallback<StudentDto> callback) {
        apiService.createStudent(student).enqueue(new Callback<StudentDto>() {
            @Override
            public void onResponse(Call<StudentDto> call, Response<StudentDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Create student failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<StudentDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- STAFF ADMINS (Neon PostgreSQL) ---

    public void getAllStaffAdmins(RepositoryCallback<List<StaffAdminDto>> callback) {
        apiService.getAllStaffAdmins().enqueue(new Callback<List<StaffAdminDto>>() {
            @Override
            public void onResponse(Call<List<StaffAdminDto>> call, Response<List<StaffAdminDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch staff admins failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<StaffAdminDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- STUDENT ADMINS (Neon PostgreSQL) ---

    public void getAllStudentAdmins(RepositoryCallback<List<StudentAdminDto>> callback) {
        apiService.getAllStudentAdmins().enqueue(new Callback<List<StudentAdminDto>>() {
            @Override
            public void onResponse(Call<List<StudentAdminDto>> call, Response<List<StudentAdminDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch student admins failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<StudentAdminDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- SUPER ADMINS (Neon PostgreSQL) ---

    public void getAllSuperAdmins(RepositoryCallback<List<SuperAdminDto>> callback) {
        apiService.getAllSuperAdmins().enqueue(new Callback<List<SuperAdminDto>>() {
            @Override
            public void onResponse(Call<List<SuperAdminDto>> call, Response<List<SuperAdminDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch super admins failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<SuperAdminDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void getSuperAdmin(String idOrEmail, RepositoryCallback<SuperAdminDto> callback) {
        apiService.getSuperAdmin(idOrEmail).enqueue(new Callback<SuperAdminDto>() {
            @Override
            public void onResponse(Call<SuperAdminDto> call, Response<SuperAdminDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch super admin failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<SuperAdminDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void createSuperAdmin(SuperAdminDto superAdmin, RepositoryCallback<SuperAdminDto> callback) {
        apiService.createSuperAdmin(superAdmin).enqueue(new Callback<SuperAdminDto>() {
            @Override
            public void onResponse(Call<SuperAdminDto> call, Response<SuperAdminDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Create super admin failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<SuperAdminDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- DEPARTMENTS (Neon PostgreSQL) ---

    public void getAllDepartments(RepositoryCallback<List<DepartmentDto>> callback) {
        apiService.getAllDepartments().enqueue(new Callback<List<DepartmentDto>>() {
            @Override
            public void onResponse(Call<List<DepartmentDto>> call, Response<List<DepartmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch departments failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<DepartmentDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }
}
