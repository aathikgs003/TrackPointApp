package com.example.trackpoint.network;

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
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TrackPointApiService {

    // --- Authentication ---
    @POST("api/auth/login")
    Call<LoginResponseDto> login(@Body LoginRequestDto loginRequest);

    // --- Announcements ---
    @GET("api/announcements")
    Call<List<AnnouncementDto>> getAnnouncements(@Query("targetRole") String targetRole);

    @POST("api/announcements")
    Call<AnnouncementDto> postAnnouncement(@Body AnnouncementDto announcement);

    // --- Departments ---
    @GET("api/departments")
    Call<List<DepartmentDto>> getAllDepartments();

    @POST("api/departments")
    Call<DepartmentDto> createDepartment(@Body DepartmentDto department);

    // --- Super Admins ---
    @GET("api/super-admins")
    Call<List<SuperAdminDto>> getAllSuperAdmins();

    @GET("api/super-admins/{idOrEmail}")
    Call<SuperAdminDto> getSuperAdmin(@Path("idOrEmail") String idOrEmail);

    @POST("api/super-admins")
    Call<SuperAdminDto> createSuperAdmin(@Body SuperAdminDto superAdmin);

    @PUT("api/super-admins/{adminId}")
    Call<SuperAdminDto> updateSuperAdmin(@Path("adminId") String adminId, @Body SuperAdminDto superAdmin);

    @DELETE("api/super-admins/{adminId}")
    Call<Void> deleteSuperAdmin(@Path("adminId") String adminId);

    // --- Staff Admins ---
    @GET("api/staff-admins")
    Call<List<StaffAdminDto>> getAllStaffAdmins();

    @GET("api/staff-admins/{idOrEmail}")
    Call<StaffAdminDto> getStaffAdmin(@Path("idOrEmail") String idOrEmail);

    @POST("api/staff-admins")
    Call<StaffAdminDto> createStaffAdmin(@Body StaffAdminDto staff);

    @PUT("api/staff-admins/{staffId}")
    Call<StaffAdminDto> updateStaffAdmin(@Path("staffId") String staffId, @Body StaffAdminDto staff);

    @DELETE("api/staff-admins/{staffId}")
    Call<Void> deleteStaffAdmin(@Path("staffId") String staffId);

    // --- Student Admins ---
    @GET("api/student-admins")
    Call<List<StudentAdminDto>> getAllStudentAdmins();

    @GET("api/student-admins/{adminId}")
    Call<StudentAdminDto> getStudentAdmin(@Path("adminId") String adminId);

    @POST("api/student-admins")
    Call<StudentAdminDto> createStudentAdmin(@Body StudentAdminDto studentAdmin);

    @PUT("api/student-admins/{adminId}")
    Call<StudentAdminDto> updateStudentAdmin(@Path("adminId") String adminId, @Body StudentAdminDto studentAdmin);

    @DELETE("api/student-admins/{adminId}")
    Call<Void> deleteStudentAdmin(@Path("adminId") String adminId);

    // --- Students ---
    @GET("api/students")
    Call<List<StudentDto>> getAllStudents();

    @GET("api/students/{regNo}")
    Call<StudentDto> getStudentByRegNo(@Path("regNo") String regNo);

    @POST("api/students")
    Call<StudentDto> createStudent(@Body StudentDto student);

    @PUT("api/students/{regNo}")
    Call<StudentDto> updateStudent(@Path("regNo") String regNo, @Body StudentDto student);

    @DELETE("api/students/{regNo}")
    Call<Void> deleteStudent(@Path("regNo") String regNo);

    // --- Attendance ---
    @GET("api/attendance/student/{regNo}")
    Call<List<AttendanceDto>> getAttendanceForStudent(@Path("regNo") String regNo);

    @GET("api/attendance/{regNo}/{dateKey}")
    Call<AttendanceDto> getAttendanceStatus(@Path("regNo") String regNo, @Path("dateKey") String dateKey);

    @POST("api/attendance")
    Call<AttendanceDto> recordAttendance(@Body AttendanceDto attendance);

    @POST("api/attendance/batch")
    Call<Void> recordAttendanceBatch(@Body List<AttendanceDto> records);

    // --- Daily Activities (Integrated with Backblaze B2 S3 Object Keys & Neon) ---
    @GET("api/activities/student/{regNo}")
    Call<List<DailyActivityDto>> getDailyActivitiesForStudent(@Path("regNo") String regNo);

    @GET("api/activities/{regNo}/{dateKey}")
    Call<DailyActivityDto> getDailyActivity(@Path("regNo") String regNo, @Path("dateKey") String dateKey);

    @POST("api/activities")
    Call<DailyActivityDto> upsertDailyActivity(@Body DailyActivityDto activity);

    @DELETE("api/activities/{regNo}/{dateKey}")
    Call<Void> deleteDailyActivity(@Path("regNo") String regNo, @Path("dateKey") String dateKey);

    @PATCH("api/activities/{regNo}/{dateKey}/remark")
    Call<DailyActivityDto> updateStaffRemark(@Path("regNo") String regNo, @Path("dateKey") String dateKey, @Query("remark") String remark);
}
