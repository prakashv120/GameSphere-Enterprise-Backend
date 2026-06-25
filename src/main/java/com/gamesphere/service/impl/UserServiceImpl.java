package com.gamesphere.service.impl;

import com.gamesphere.dto.request.LoginRequest;
import com.gamesphere.dto.request.RegisterRequest;
import com.gamesphere.dto.request.UserProfileUpdateRequest;
import com.gamesphere.dto.response.AuthResponse;
import com.gamesphere.dto.response.UserProfileResponse;
import com.gamesphere.entity.User;
import com.gamesphere.entity.UserProfile;
import com.gamesphere.enums.Role;
import com.gamesphere.exception.BadRequestException;
import com.gamesphere.exception.ResourceNotFoundException;
import com.gamesphere.mapper.UserMapper;
import com.gamesphere.repository.UserProfileRepository;
import com.gamesphere.repository.UserRepository;
import com.gamesphere.security.JwtTokenProvider;
import com.gamesphere.security.UserPrincipal;
import com.gamesphere.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public UserServiceImpl(UserRepository userRepository,
                            UserProfileRepository userProfileRepository,
                            PasswordEncoder passwordEncoder,
                            JwtTokenProvider tokenProvider,
                            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        log.info("Attempting to register user: {}", registerRequest.getUsername());
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Username already exists: {}", registerRequest.getUsername());
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Email already exists: {}", registerRequest.getEmail());
            throw new BadRequestException("Email is already in use!");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .build();

        UserProfile profile = UserProfile.builder()
                .user(user)
                .fullName("")
                .bio("")
                .avatarUrl("")
                .phone("")
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .build();

        user.setUserProfile(profile);
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // Generate authentication programmatically to return token
        UserPrincipal principal = UserPrincipal.create(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginUser(LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            log.info("User logged in successfully: {}", principal.getUsername());

            return AuthResponse.builder()
                    .token(token)
                    .id(principal.getId())
                    .username(principal.getUsername())
                    .email(principal.getEmail())
                    .role(Role.valueOf(principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")))
                    .build();
        } catch (Exception ex) {
            log.error("Authentication failed for user {}: {}", loginRequest.getUsername(), ex.getMessage());
            throw new BadRequestException("Invalid username or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long id, UserProfileUpdateRequest updateRequest) {
        log.info("Updating profile for user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
            user.setUserProfile(profile);
        }

        if (updateRequest.getFullName() != null) {
            profile.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getBio() != null) {
            profile.setBio(updateRequest.getBio());
        }
        if (updateRequest.getAvatarUrl() != null) {
            profile.setAvatarUrl(updateRequest.getAvatarUrl());
        }
        if (updateRequest.getPhone() != null) {
            profile.setPhone(updateRequest.getPhone());
        }

        userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", id);
        return UserMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        log.info("Soft-deleting account for user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
        log.info("Account soft-deleted successfully for user ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllPlayers(Pageable pageable) {
        return userRepository.findByRole(Role.PLAYER, pageable)
                .map(UserMapper::toProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchPlayersByName(String name, Pageable pageable) {
        return userRepository.findByRoleAndUsernameContainingIgnoreCase(Role.PLAYER, name, pageable)
                .map(UserMapper::toProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> filterPlayersByTeam(Long teamId, Pageable pageable) {
        return userRepository.findByRoleAndTeamId(Role.PLAYER, teamId, pageable)
                .map(UserMapper::toProfileResponse);
    }
}
