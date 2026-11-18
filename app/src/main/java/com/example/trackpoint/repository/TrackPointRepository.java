package com.example.trackpoint.repository;

import android.content.Context;
import android.net.Uri;
import com.example.trackpoint.S3StorageManager;
import com.example.trackpoint.network.ApiClient;
import com.example.trackpoint.network.TrackPointApiService;
import com.example.trackpoint.network.dto.AnnouncementDto;
import com.example.trackpoint.network.dto.AttendanceDto;
import com.example.trackpoint.network.dto.DailyActivityDto;
import com.example.trackpoint.network.dto.DepartmentDto;
import com.example.trackpoint.network.dto.LoginRequestDto;
import com.example.trackpoint.network.dto.LoginResponseDto;
import com.example.trackpoint.network.dto.StaffAdminDto;
import com.example.trackpoint.network.dto.StudentAdminDto;
import com.example.trackpoint.network.dto.StudentDto;
import com.example.trackpoint.network.dto.SuperAdminDto;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackPointRepository {

    private static TrackPointRepository instance;
    private final TrackPointApiService apiService;
    private final S3StorageManager s3Manager;

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public TrackPointRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.s3Manager = S3StorageManager.getInstance(context);
    }

    public static synchronized TrackPointRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TrackPointRepository(context.getApplicationContext());
        }
        return instance;
    }

    // --- AUTHENTICATION ---

    public void login(String email, String password, String role, RepositoryCallback<LoginResponseDto> callback) {
        LoginRequestDto request = new LoginRequestDto(email, password, role);
        apiService.login(request).enqueue(new Callback<LoginResponseDto>() {
            @Override
            public void onResponse(Call<LoginResponseDto> call, Response<LoginResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Invalid credentials or server error (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<LoginResponseDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- ANNOUNCEMENTS ---

    public void getAnnouncements(String targetRole, RepositoryCallback<List<AnnouncementDto>> callback) {
        apiService.getAnnouncements(targetRole).enqueue(new Callback<List<AnnouncementDto>>() {
            @Override
            public void onResponse(Call<List<AnnouncementDto>> call, Response<List<AnnouncementDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Failed to fetch announcements (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<List<AnnouncementDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void postAnnouncement(AnnouncementDto announcement, RepositoryCallback<AnnouncementDto> callback) {
        apiService.postAnnouncement(announcement).enqueue(new Callback<AnnouncementDto>() {
            @Override
            public void onResponse(Call<AnnouncementDto> call, Response<AnnouncementDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Failed to post announcement (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<AnnouncementDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- STUDENTS ---

    public void getAllStudents(RepositoryCallback<List<StudentDto>> callback) {
        apiService.getAllStudents().enqueue(new Callback<List<StudentDto>>() {
            @Override
            public void onResponse(Call<List<StudentDto>> call, Response<List<StudentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Failed to fetch students (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<List<StudentDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void getStudentByRegNo(String regNo, RepositoryCallback<StudentDto> callback) {
        apiService.getStudentByRegNo(regNo).enqueue(new Callback<StudentDto>() {
            @Override
            public void onResponse(Call<StudentDto> call, Response<StudentDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Student not found (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<StudentDto> call, Throwable t) {
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
                    if (callback != null) callback.onError(new Exception("Create student failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<StudentDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void updateStudent(String regNo, StudentDto student, RepositoryCallback<StudentDto> callback) {
        apiService.updateStudent(regNo, student).enqueue(new Callback<StudentDto>() {
            @Override
            public void onResponse(Call<StudentDto> call, Response<StudentDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Update student failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<StudentDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void deleteStudent(String regNo, RepositoryCallback<Void> callback) {
        apiService.deleteStudent(regNo).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess(null);
                } else {
                    if (callback != null) callback.onError(new Exception("Delete student failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- STUDENT ADMINS ---

    public void getAllStudentAdmins(RepositoryCallback<List<StudentAdminDto>> callback) {
        apiService.getAllStudentAdmins().enqueue(new Callback<List<StudentAdminDto>>() {
            @Override
            public void onResponse(Call<List<StudentAdminDto>> call, Response<List<StudentAdminDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch student admins failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<List<StudentAdminDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void createStudentAdmin(StudentAdminDto studentAdmin, RepositoryCallback<StudentAdminDto> callback) {
        apiService.createStudentAdmin(studentAdmin).enqueue(new Callback<StudentAdminDto>() {
            @Override
            public void onResponse(Call<StudentAdminDto> call, Response<StudentAdminDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Create student admin failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<StudentAdminDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- STAFF ADMINS ---

    public void getAllStaffAdmins(RepositoryCallback<List<StaffAdminDto>> callback) {
        apiService.getAllStaffAdmins().enqueue(new Callback<List<StaffAdminDto>>() {
            @Override
            public void onResponse(Call<List<StaffAdminDto>> call, Response<List<StaffAdminDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch staff admins failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<List<StaffAdminDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void createStaffAdmin(StaffAdminDto staff, RepositoryCallback<StaffAdminDto> callback) {
        apiService.createStaffAdmin(staff).enqueue(new Callback<StaffAdminDto>() {
            @Override
            public void onResponse(Call<StaffAdminDto> call, Response<StaffAdminDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Create staff admin failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<StaffAdminDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- SUPER ADMINS ---

    public void getAllSuperAdmins(RepositoryCallback<List<SuperAdminDto>> callback) {
        apiService.getAllSuperAdmins().enqueue(new Callback<List<SuperAdminDto>>() {
            @Override
            public void onResponse(Call<List<SuperAdminDto>> call, Response<List<SuperAdminDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch super admins failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<List<SuperAdminDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- ATTENDANCE ---

    public void getAttendanceStatus(String regNo, String dateKey, RepositoryCallback<AttendanceDto> callback) {
        apiService.getAttendanceStatus(regNo, dateKey).enqueue(new Callback<AttendanceDto>() {
            @Override
            public void onResponse(Call<AttendanceDto> call, Response<AttendanceDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch attendance failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<AttendanceDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void recordAttendance(AttendanceDto attendance, RepositoryCallback<AttendanceDto> callback) {
        apiService.recordAttendance(attendance).enqueue(new Callback<AttendanceDto>() {
            @Override
            public void onResponse(Call<AttendanceDto> call, Response<AttendanceDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Record attendance failed (" + response.code() + ")"));
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
                    if (callback != null) callback.onError(new Exception("Batch attendance failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- DAILY ACTIVITIES & BACKBLAZE B2 ---

    public void submitDailyActivity(Context context, Uri fileUri, String studentRegNo, String dateKey, String notes, String submitTime, RepositoryCallback<DailyActivityDto> callback) {
        if (fileUri == null) {
            DailyActivityDto dto = new DailyActivityDto(studentRegNo, dateKey, "", "", "", notes, submitTime);
            saveActivityToNeon(dto, callback);
            return;
        }

        s3Manager.uploadFile(context, fileUri, studentRegNo, dateKey, new S3StorageManager.S3UploadCallback() {
            @Override
            public void onSuccess(String objectKey, String presignedUrl) {
                String fileName = objectKey.contains("_") ? objectKey.substring(objectKey.indexOf("_") + 1) : "upload_file";
                DailyActivityDto dto = new DailyActivityDto(studentRegNo, dateKey, fileName, objectKey, presignedUrl, notes, submitTime);
                saveActivityToNeon(dto, callback);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    private void saveActivityToNeon(DailyActivityDto dto, RepositoryCallback<DailyActivityDto> callback) {
        apiService.upsertDailyActivity(dto).enqueue(new Callback<DailyActivityDto>() {
            @Override
            public void onResponse(Call<DailyActivityDto> call, Response<DailyActivityDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Activity submission failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void getDailyActivity(String studentRegNo, String dateKey, RepositoryCallback<DailyActivityDto> callback) {
        apiService.getDailyActivity(studentRegNo, dateKey).enqueue(new Callback<DailyActivityDto>() {
            @Override
            public void onResponse(Call<DailyActivityDto> call, Response<DailyActivityDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DailyActivityDto activity = response.body();
                    if (activity.getFileKey() != null && !activity.getFileKey().isEmpty()) {
                        String freshPresignedUrl = s3Manager.generatePresignedUrl(activity.getFileKey(), 60);
                        activity.setFileUrl(freshPresignedUrl);
                    }
                    if (callback != null) callback.onSuccess(activity);
                } else {
                    if (callback != null) callback.onError(new Exception("Activity not found (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    public void deleteDailyActivity(String studentRegNo, String dateKey, String fileKey, RepositoryCallback<Void> callback) {
        if (fileKey != null && !fileKey.isEmpty()) {
            s3Manager.deleteFile(fileKey, new S3StorageManager.S3DeleteCallback() {
                @Override
                public void onSuccess() {
                    deleteActivityFromNeon(studentRegNo, dateKey, callback);
                }

                @Override
                public void onError(Exception e) {
                    deleteActivityFromNeon(studentRegNo, dateKey, callback);
                }
            });
        } else {
            deleteActivityFromNeon(studentRegNo, dateKey, callback);
        }
    }

    private void deleteActivityFromNeon(String studentRegNo, String dateKey, RepositoryCallback<Void> callback) {
        apiService.deleteDailyActivity(studentRegNo, dateKey).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess(null);
                } else {
                    if (callback != null) callback.onError(new Exception("Delete activity failed (" + response.code() + ")"));
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
                    if (callback != null) callback.onError(new Exception("Update remark failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityDto> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }

    // --- DEPARTMENTS ---

    public void getAllDepartments(RepositoryCallback<List<DepartmentDto>> callback) {
        apiService.getAllDepartments().enqueue(new Callback<List<DepartmentDto>>() {
            @Override
            public void onResponse(Call<List<DepartmentDto>> call, Response<List<DepartmentDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) callback.onSuccess(response.body());
                } else {
                    if (callback != null) callback.onError(new Exception("Fetch departments failed (" + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<List<DepartmentDto>> call, Throwable t) {
                if (callback != null) callback.onError(new Exception(t));
            }
        });
    }
}
