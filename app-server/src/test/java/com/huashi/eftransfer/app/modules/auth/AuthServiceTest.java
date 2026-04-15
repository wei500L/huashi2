package com.huashi.eftransfer.app.modules.auth;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import com.huashi.eftransfer.app.common.config.AuthRegistrationProperties;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.security.store.RegistrationContextSession;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassStudentMapper;
import com.huashi.eftransfer.app.modules.analytics.service.TeachingClassService;
import com.huashi.eftransfer.app.modules.auth.dto.RegisterStudentRequest;
import com.huashi.eftransfer.app.modules.auth.service.AuthLockoutService;
import com.huashi.eftransfer.app.modules.auth.service.AuthService;
import com.huashi.eftransfer.app.modules.auth.support.AuthClientContext;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.modules.user.service.UserQueryService;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private StudentProfileMapper studentProfileMapper;

    @Mock
    private TeachingClassService teachingClassService;

    @Mock
    private TeachingClassStudentMapper teachingClassStudentMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthRegistrationProperties authRegistrationProperties;

    @Mock
    private AuthTokenStore authTokenStore;

    @Mock
    private AuthLockoutService authLockoutService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldTranslateConcurrentRegistrationConflictToConflictResponse() {
        RegisterStudentRequest request = registerRequest();
        TeachingClassEntity teachingClass = activeClass();
        DataIntegrityViolationException duplicateInsert = new DataIntegrityViolationException("duplicate user");
        UserEntity existingUser = new UserEntity();
        existingUser.setUsername(request.username());
        RegistrationContextSession registrationContextSession = registrationContextSession("10.0.0.51", "junit");

        when(authTokenStore.acquireRegistrationContextLock(org.mockito.ArgumentMatchers.anyString(), any()))
                .thenReturn(true);
        when(authTokenStore.findRegistrationContextSession(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(registrationContextSession));
        when(userQueryService.findByUsernameOrEmail(request.username()))
                .thenReturn(Optional.empty(), Optional.of(existingUser));
        when(userQueryService.findByUsernameOrEmail(request.email()))
                .thenReturn(Optional.empty());
        when(teachingClassService.findActiveByClassCode("CLS-0001"))
                .thenReturn(Optional.of(teachingClass));
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");
        when(userMapper.insert(any(UserEntity.class))).thenThrow(duplicateInsert);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.registerStudent(request, new AuthClientContext("10.0.0.51", "junit"))
        );

        assertEquals(ResultCode.CONFLICT, exception.getResultCode());
        assertEquals(409, exception.getHttpStatus());
        assertEquals("username already exists", exception.getMessage());
        verify(authTokenStore).releaseRegistrationContextLock(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRethrowUnexpectedIntegrityViolationDuringRegistration() {
        RegisterStudentRequest request = registerRequest();
        TeachingClassEntity teachingClass = activeClass();
        DataIntegrityViolationException unexpectedFailure = new DataIntegrityViolationException("foreign key failure");
        RegistrationContextSession registrationContextSession = registrationContextSession("10.0.0.52", "junit");

        when(authTokenStore.acquireRegistrationContextLock(org.mockito.ArgumentMatchers.anyString(), any()))
                .thenReturn(true);
        when(authTokenStore.findRegistrationContextSession(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(registrationContextSession));
        when(userQueryService.findByUsernameOrEmail(request.username()))
                .thenReturn(Optional.empty(), Optional.empty());
        when(userQueryService.findByUsernameOrEmail(request.email()))
                .thenReturn(Optional.empty(), Optional.empty());
        when(teachingClassService.findActiveByClassCode("CLS-0001"))
                .thenReturn(Optional.of(teachingClass));
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");
        when(userMapper.insert(any(UserEntity.class))).thenThrow(unexpectedFailure);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> authService.registerStudent(request, new AuthClientContext("10.0.0.52", "junit"))
        );

        assertSame(unexpectedFailure, exception);
        verify(authTokenStore).releaseRegistrationContextLock(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectRegistrationWhenRegistrationContextIsAlreadyLocked() {
        RegisterStudentRequest request = registerRequest();

        when(authTokenStore.acquireRegistrationContextLock(org.mockito.ArgumentMatchers.anyString(), any()))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.registerStudent(request, new AuthClientContext("10.0.0.52", "junit"))
        );

        assertEquals(ResultCode.REGISTRATION_CONTEXT_BUSY, exception.getResultCode());
        assertEquals(409, exception.getHttpStatus());
        assertEquals(
                "Registration is already in progress for this verification. Wait a moment and try again.",
                exception.getMessage()
        );
    }

    @Test
    void shouldKeepRegistrationContextAvailableWhenTokenIssuanceFails() {
        RegisterStudentRequest request = registerRequest();
        TeachingClassEntity teachingClass = activeClass();
        RegistrationContextSession registrationContextSession = registrationContextSession("10.0.0.53", "junit");

        when(authTokenStore.acquireRegistrationContextLock(org.mockito.ArgumentMatchers.anyString(), any()))
                .thenReturn(true);
        when(authTokenStore.findRegistrationContextSession(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(registrationContextSession));
        when(userQueryService.findByUsernameOrEmail(request.username()))
                .thenReturn(Optional.empty());
        when(userQueryService.findByUsernameOrEmail(request.email()))
                .thenReturn(Optional.empty());
        when(teachingClassService.findActiveByClassCode("CLS-0001"))
                .thenReturn(Optional.of(teachingClass));
        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");
        when(studentProfileMapper.selectCount(any()))
                .thenReturn(0L);
        doAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(200L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));
        when(jwtTokenProvider.generateAccessToken(eq(200L), eq(request.username()), eq(request.displayName()), any()))
                .thenThrow(new IllegalStateException("token issue failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.registerStudent(request, new AuthClientContext("10.0.0.53", "junit"))
        );

        assertEquals("token issue failed", exception.getMessage());
        verify(authTokenStore, never()).revokeRegistrationContextSession(org.mockito.ArgumentMatchers.anyString());
        verify(authTokenStore).releaseRegistrationContextLock(org.mockito.ArgumentMatchers.anyString());
    }

    private RegisterStudentRequest registerRequest() {
        return new RegisterStudentRequest(
                "student.self",
                "student.self@ef.local",
                "Self Register Student",
                "Student@123456",
                "registration-token",
                "B1",
                "A2",
                "FOUNDATION"
        );
    }

    private RegistrationContextSession registrationContextSession(String issuedIpAddress, String userAgentFingerprint) {
        return new RegistrationContextSession(
                "token-hash",
                "CLS-0001",
                "Pilot Class",
                "Pilot Grade",
                java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(600),
                issuedIpAddress,
                userAgentFingerprint
        );
    }

    private TeachingClassEntity activeClass() {
        TeachingClassEntity teachingClass = new TeachingClassEntity();
        teachingClass.setId(1L);
        teachingClass.setClassCode("CLS-0001");
        teachingClass.setClassName("Pilot Class");
        teachingClass.setGradeName("Pilot Grade");
        teachingClass.setActive(Boolean.TRUE);
        return teachingClass;
    }
}
