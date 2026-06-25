package com.gamesphere.service.impl;

import com.gamesphere.dto.request.CreateTeamRequest;
import com.gamesphere.dto.request.UpdateTeamRequest;
import com.gamesphere.dto.response.TeamResponse;
import com.gamesphere.entity.Team;
import com.gamesphere.entity.User;
import com.gamesphere.exception.BadRequestException;
import com.gamesphere.exception.ResourceNotFoundException;
import com.gamesphere.exception.UnauthorizedException;
import com.gamesphere.mapper.TeamMapper;
import com.gamesphere.repository.TeamRepository;
import com.gamesphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TeamMapper teamMapper = new TeamMapper();

    @InjectMocks
    private TeamServiceImpl teamService;

    private User user;
    private Team team;
    private CreateTeamRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("captain")
                .email("captain@gamesphere.com")
                .build();

        team = Team.builder()
                .id(10L)
                .name("Alpha Squad")
                .tag("ALPHA")
                .description("Pro team")
                .captain(user)
                .members(new ArrayList<>(Collections.singletonList(user)))
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .build();

        createRequest = new CreateTeamRequest();
        createRequest.setName("Alpha Squad");
        createRequest.setTag("ALPHA");
        createRequest.setDescription("Pro team");
    }

    @Test
    void createTeam_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(teamRepository.existsByName("Alpha Squad")).thenReturn(false);
        when(teamRepository.existsByTag("ALPHA")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamResponse response = teamService.createTeam(1L, createRequest);

        assertNotNull(response);
        assertEquals("Alpha Squad", response.getName());
        assertEquals("ALPHA", response.getTag());
        verify(teamRepository, times(1)).save(any(Team.class));
    }

    @Test
    void createTeam_AlreadyInTeam_ThrowsException() {
        user.setTeam(team); // Already in a team
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> teamService.createTeam(1L, createRequest));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void getTeamById_Success() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamResponse response = teamService.getTeamById(10L);

        assertNotNull(response);
        assertEquals("Alpha Squad", response.getName());
    }

    @Test
    void getTeamById_NotFound_ThrowsException() {
        when(teamRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teamService.getTeamById(10L));
    }

    @Test
    void joinTeam_Success() {
        User joiner = User.builder().id(2L).username("joiner").build();
        Team targetTeam = Team.builder()
                .id(10L)
                .name("Alpha Squad")
                .tag("ALPHA")
                .members(new ArrayList<>(Collections.singletonList(user)))
                .build();

        when(teamRepository.findById(10L)).thenReturn(Optional.of(targetTeam));
        when(userRepository.findById(2L)).thenReturn(Optional.of(joiner));

        TeamResponse response = teamService.joinTeam(10L, 2L);

        assertNotNull(response);
        assertEquals(2, response.getMemberCount());
        verify(userRepository, times(1)).save(joiner);
    }

    @Test
    void joinTeam_AlreadyInTeam_ThrowsException() {
        User joiner = User.builder().id(2L).username("joiner").team(team).build(); // already in team
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(2L)).thenReturn(Optional.of(joiner));

        assertThrows(BadRequestException.class, () -> teamService.joinTeam(10L, 2L));
    }

    @Test
    void leaveTeam_Success() {
        User member = User.builder().id(2L).username("member").team(team).build();
        team.getMembers().add(member);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));

        teamService.leaveTeam(10L, 2L);

        assertFalse(team.getMembers().contains(member));
        verify(userRepository, times(1)).save(member);
    }

    @Test
    void leaveTeam_CaptainLeaves_ThrowsException() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> teamService.leaveTeam(10L, 1L));
    }

    @Test
    void updateTeam_Success() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        UpdateTeamRequest updateReq = new UpdateTeamRequest();
        updateReq.setDescription("New Description");

        TeamResponse response = teamService.updateTeam(10L, 1L, updateReq);

        assertNotNull(response);
        assertEquals("New Description", response.getDescription());
        verify(teamRepository, times(1)).save(team);
    }

    @Test
    void updateTeam_Unauthorized_ThrowsException() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        UpdateTeamRequest updateReq = new UpdateTeamRequest();

        assertThrows(UnauthorizedException.class, () -> teamService.updateTeam(10L, 2L, updateReq)); // user 2 is not captain
    }

    @Test
    void deleteTeam_Success() {
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        teamService.deleteTeam(10L, 1L);

        verify(teamRepository, times(1)).delete(team);
    }
}
