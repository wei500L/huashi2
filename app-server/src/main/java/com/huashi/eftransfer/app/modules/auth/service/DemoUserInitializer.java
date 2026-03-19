package com.huashi.eftransfer.app.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.TeacherProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.TeacherProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.shared.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            StudentProfileMapper studentProfileMapper,
            TeacherProfileMapper teacherProfileMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserEntity admin = ensureUser("admin", "admin@ef.local", "System Admin", "Admin@123456");
        ensureRole(admin.getId(), UserRole.ADMIN);

        UserEntity teacher = ensureUser("teacher.zhang", "teacher.zhang@ef.local", "张老师", "Teacher@123456");
        ensureRole(teacher.getId(), UserRole.TEACHER);
        ensureTeacherProfile(teacher.getId(), "T-0001", "Applied Linguistics", "Associate Professor");

        UserEntity studentOne = ensureUser("student.li", "student.li@ef.local", "李华", "Student@123456");
        ensureRole(studentOne.getId(), UserRole.STUDENT);
        ensureStudentProfile(studentOne.getId(), "S-1001", "Grade 10", "B2", "B1", 72);

        UserEntity studentTwo = ensureUser("student.wang", "student.wang@ef.local", "王敏", "Student@123456");
        ensureRole(studentTwo.getId(), UserRole.STUDENT);
        ensureStudentProfile(studentTwo.getId(), "S-1002", "Grade 11", "B1", "A2", 63);

        log.info("event=demo_users_ready accounts=4 usernames=admin,teacher.zhang,student.li,student.wang");
    }

    private UserEntity ensureUser(String username, String email, String displayName, String rawPassword) {
        UserEntity existing = userMapper.selectByUsernameOrEmail(username);
        if (existing != null) {
            return existing;
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(Boolean.TRUE);
        user.setCreatedBy(0L);
        user.setUpdatedBy(0L);
        userMapper.insert(user);
        return user;
    }

    private void ensureRole(Long userId, UserRole role) {
        Long count = userRoleMapper.selectCount(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getUserId, userId)
                .eq(UserRoleEntity::getRoleCode, role.name()));
        if (count != null && count > 0) {
            return;
        }
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(userId);
        userRole.setRoleCode(role.name());
        userRole.setCreatedBy(0L);
        userRole.setUpdatedBy(0L);
        userRoleMapper.insert(userRole);
    }

    private void ensureStudentProfile(
            Long userId,
            String studentNo,
            String gradeName,
            String englishLevel,
            String frenchLevel,
            int compositeScore
    ) {
        Long count = studentProfileMapper.selectCount(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, userId));
        if (count != null && count > 0) {
            return;
        }
        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setUserId(userId);
        profile.setStudentNo(studentNo);
        profile.setGradeName(gradeName);
        profile.setEnglishLevel(englishLevel);
        profile.setFrenchLevel(frenchLevel);
        profile.setCompositeScore(compositeScore);
        profile.setCreatedBy(0L);
        profile.setUpdatedBy(0L);
        studentProfileMapper.insert(profile);
    }

    private void ensureTeacherProfile(Long userId, String employeeNo, String department, String title) {
        Long count = teacherProfileMapper.selectCount(Wrappers.<TeacherProfileEntity>lambdaQuery()
                .eq(TeacherProfileEntity::getUserId, userId));
        if (count != null && count > 0) {
            return;
        }
        TeacherProfileEntity profile = new TeacherProfileEntity();
        profile.setUserId(userId);
        profile.setEmployeeNo(employeeNo);
        profile.setDepartment(department);
        profile.setTitle(title);
        profile.setCreatedBy(0L);
        profile.setUpdatedBy(0L);
        teacherProfileMapper.insert(profile);
    }
}
