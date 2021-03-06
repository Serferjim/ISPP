package com.ispp.thorneo.web.rest;

import com.ispp.thorneo.ThorneoApp;

import com.ispp.thorneo.domain.Tournament;
import com.ispp.thorneo.domain.Participation;
import com.ispp.thorneo.domain.Game;
import com.ispp.thorneo.domain.User;
import com.ispp.thorneo.repository.TournamentRepository;
import com.ispp.thorneo.repository.search.TournamentSearchRepository;
import com.ispp.thorneo.security.SecurityUtils;
import com.ispp.thorneo.service.TournamentService;
import com.ispp.thorneo.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static com.ispp.thorneo.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ispp.thorneo.domain.enumeration.Type;
/**
 * Test class for the TournamentResource REST controller.
 *
 * @see TournamentResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ThorneoApp.class)
public class TournamentResourceIntTest {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_MEETING_DATE = Instant.now().plusSeconds(55000);
    private static final Instant UPDATED_MEETING_DATE = Instant.now().plusSeconds(85000);

    private static final String DEFAULT_MEETING_POINT = "AAAAAAAAAA";
    private static final String UPDATED_MEETING_POINT = "BBBBBBBBBB";

    private static final String DEFAULT_CITY = "AAAAAAAAAA";
    private static final String UPDATED_CITY = "BBBBBBBBBB";

    private static final Integer DEFAULT_PRICE = 0;
    private static final Integer UPDATED_PRICE = 1;

    private static final Integer DEFAULT_PLAYER_SIZE = 2;
    private static final Integer UPDATED_PLAYER_SIZE = 3;

    private static final String DEFAULT_REWARDS = "AAAAAAAAAA";
    private static final String UPDATED_REWARDS = "BBBBBBBBBB";

    private static final String DEFAULT_IMAGE_URL = "http://www.google.com";
    private static final String UPDATED_IMAGE_URL = "http://www.google.es";

    private static final Long DEFAULT_LATITUDE = 1L;
    private static final Long UPDATED_LATITUDE = 2L;

    private static final Long DEFAULT_LONGITUDE = 1L;
    private static final Long UPDATED_LONGITUDE = 2L;

    private static final Type DEFAULT_TYPE = Type.ELIMINATION;
    private static final Type UPDATED_TYPE = Type.POINT;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentService tournamentService;

    /**
     * This repository is mocked in the com.ispp.thorneo.repository.search test package.
     *
     * @see com.ispp.thorneo.repository.search.TournamentSearchRepositoryMockConfiguration
     */
    @Autowired
    private TournamentSearchRepository mockTournamentSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    @Autowired
    private UserDetailsService domainUserDetailsService;

    private MockMvc restTournamentMockMvc;

    private Tournament tournament;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final TournamentResource tournamentResource = new TournamentResource(tournamentService);
        this.restTournamentMockMvc = MockMvcBuilders.standaloneSetup(tournamentResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Tournament createEntity(EntityManager em) {
        Tournament tournament = new Tournament()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .meetingDate(DEFAULT_MEETING_DATE)
            .meetingPoint(DEFAULT_MEETING_POINT)
            .city(DEFAULT_CITY)
            .price(DEFAULT_PRICE)
            .playerSize(DEFAULT_PLAYER_SIZE)
            .rewards(DEFAULT_REWARDS)
            .imageUrl(DEFAULT_IMAGE_URL)
            .latitude(DEFAULT_LATITUDE)
            .longitude(DEFAULT_LONGITUDE)
            .type(DEFAULT_TYPE);
        // Add required entity
        Participation participation = ParticipationResourceIntTest.createEntity(em);
        em.persist(participation);
        em.flush();
        tournament.getParticipations().add(participation);
        Game game = GameResourceIntTest.createEntity(em);
        em.persist(game);
        tournament.setGame(game);
        User user = UserResourceIntTest.createEntity(em);
        em.persist(user);
        tournament.setUser(user);
        return tournament;
    }

    @Before
    public void initTest() {
        tournament = createEntity(em);
    }

    @Test
    @Transactional
    public void createTournament() throws Exception {
        int databaseSizeBeforeCreate = tournamentRepository.findAll().size();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(tournament.getUser().getLogin(),
            tournament.getUser().getPassword()));
        SecurityContextHolder.setContext(securityContext);

        // Create the Tournament
        this.tournamentService.saveTournament(tournament);

        // Validate the Tournament in the database
        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeCreate + 1);
        Tournament testTournament = tournamentList.get(tournamentList.size() - 1);
        assertThat(testTournament.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testTournament.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testTournament.getMeetingDate()).isEqualTo(DEFAULT_MEETING_DATE);
        assertThat(testTournament.getMeetingPoint()).isEqualTo(DEFAULT_MEETING_POINT);
        assertThat(testTournament.getCity()).isEqualTo(DEFAULT_CITY);
        assertThat(testTournament.getPrice()).isEqualTo(DEFAULT_PRICE);
        assertThat(testTournament.getPlayerSize()).isEqualTo(DEFAULT_PLAYER_SIZE);
        assertThat(testTournament.getRewards()).isEqualTo(DEFAULT_REWARDS);
        assertThat(testTournament.getImageUrl()).isEqualTo(DEFAULT_IMAGE_URL);
        assertThat(testTournament.getLatitude()).isEqualTo(DEFAULT_LATITUDE);
        assertThat(testTournament.getLongitude()).isEqualTo(DEFAULT_LONGITUDE);
        assertThat(testTournament.getType()).isEqualTo(DEFAULT_TYPE);

        // Validate the Tournament in Elasticsearch
        verify(mockTournamentSearchRepository, times(1)).save(testTournament);
    }

    @Test
    @Transactional
    public void createTournamentWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = tournamentRepository.findAll().size();

        // Create the Tournament with an existing ID
        tournament.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        // Validate the Tournament in the database
        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeCreate);

        // Validate the Tournament in Elasticsearch
        verify(mockTournamentSearchRepository, times(0)).save(tournament);
    }

    @Test
    @Transactional
    public void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = tournamentRepository.findAll().size();
        // set the field null
        tournament.setTitle(null);

        // Create the Tournament, which fails.

        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDescriptionIsRequired() throws Exception {
        int databaseSizeBeforeTest = tournamentRepository.findAll().size();
        // set the field null
        tournament.setDescription(null);

        // Create the Tournament, which fails.

        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkMeetingDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = tournamentRepository.findAll().size();
        // set the field null
        tournament.setMeetingDate(null);

        // Create the Tournament, which fails.

        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkMeetingPointIsRequired() throws Exception {
        int databaseSizeBeforeTest = tournamentRepository.findAll().size();
        // set the field null
        tournament.setMeetingPoint(null);

        // Create the Tournament, which fails.

        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCityIsRequired() throws Exception {
        int databaseSizeBeforeTest = tournamentRepository.findAll().size();
        // set the field null
        tournament.setCity(null);

        // Create the Tournament, which fails.

        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkImageUrlIsRequired() throws Exception {
        int databaseSizeBeforeTest = tournamentRepository.findAll().size();
        // set the field null
        tournament.setImageUrl(null);

        // Create the Tournament, which fails.

        restTournamentMockMvc.perform(post("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllTournaments() throws Exception {
        // Initialize the database
        tournamentRepository.saveAndFlush(tournament);

        // Get all the tournamentList
        restTournamentMockMvc.perform(get("/api/tournaments?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tournament.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].meetingDate").value(hasItem(DEFAULT_MEETING_DATE.toString())))
            .andExpect(jsonPath("$.[*].meetingPoint").value(hasItem(DEFAULT_MEETING_POINT.toString())))
            .andExpect(jsonPath("$.[*].city").value(hasItem(DEFAULT_CITY.toString())))
            .andExpect(jsonPath("$.[*].price").value(hasItem(DEFAULT_PRICE)))
            .andExpect(jsonPath("$.[*].playerSize").value(hasItem(DEFAULT_PLAYER_SIZE)))
            .andExpect(jsonPath("$.[*].rewards").value(hasItem(DEFAULT_REWARDS.toString())))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL.toString())))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(DEFAULT_LATITUDE.intValue())))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(DEFAULT_LONGITUDE.intValue())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())));
    }
    
    @Test
    @Transactional
    public void getTournament() throws Exception {
        // Initialize the database
        tournamentRepository.saveAndFlush(tournament);

        // Get the tournament
        restTournamentMockMvc.perform(get("/api/tournaments/{id}", tournament.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(tournament.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.meetingDate").value(DEFAULT_MEETING_DATE.toString()))
            .andExpect(jsonPath("$.meetingPoint").value(DEFAULT_MEETING_POINT.toString()))
            .andExpect(jsonPath("$.city").value(DEFAULT_CITY.toString()))
            .andExpect(jsonPath("$.price").value(DEFAULT_PRICE))
            .andExpect(jsonPath("$.playerSize").value(DEFAULT_PLAYER_SIZE))
            .andExpect(jsonPath("$.rewards").value(DEFAULT_REWARDS.toString()))
            .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGE_URL.toString()))
            .andExpect(jsonPath("$.latitude").value(DEFAULT_LATITUDE.intValue()))
            .andExpect(jsonPath("$.longitude").value(DEFAULT_LONGITUDE.intValue()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingTournament() throws Exception {
        // Get the tournament
        restTournamentMockMvc.perform(get("/api/tournaments/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTournament() throws Exception {
        // Initialize the database
        tournamentService.save(tournament);
        // As the test used the service layer, reset the Elasticsearch mock repository
        reset(mockTournamentSearchRepository);

        int databaseSizeBeforeUpdate = tournamentRepository.findAll().size();

        // Update the tournament
        Tournament updatedTournament = tournamentRepository.findById(tournament.getId()).get();
        // Disconnect from session so that the updates on updatedTournament are not directly saved in db
        em.detach(updatedTournament);
        updatedTournament
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .meetingDate(UPDATED_MEETING_DATE)
            .meetingPoint(UPDATED_MEETING_POINT)
            .city(UPDATED_CITY)
            .price(UPDATED_PRICE)
            .playerSize(UPDATED_PLAYER_SIZE)
            .rewards(UPDATED_REWARDS)
            .imageUrl(UPDATED_IMAGE_URL)
            .latitude(UPDATED_LATITUDE)
            .longitude(UPDATED_LONGITUDE)
            .type(UPDATED_TYPE);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(updatedTournament.getUser().getLogin(),
            updatedTournament.getUser().getPassword()));
        SecurityContextHolder.setContext(securityContext);
        this.tournamentService.saveTournament(updatedTournament);

        // Validate the Tournament in the database
        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeUpdate);
        Tournament testTournament = tournamentList.get(tournamentList.size() - 1);
        assertThat(testTournament.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testTournament.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTournament.getMeetingDate()).isEqualTo(UPDATED_MEETING_DATE);
        assertThat(testTournament.getMeetingPoint()).isEqualTo(UPDATED_MEETING_POINT);
        assertThat(testTournament.getCity()).isEqualTo(UPDATED_CITY);
        assertThat(testTournament.getPrice()).isEqualTo(UPDATED_PRICE);
        assertThat(testTournament.getPlayerSize()).isEqualTo(UPDATED_PLAYER_SIZE);
        assertThat(testTournament.getRewards()).isEqualTo(UPDATED_REWARDS);
        assertThat(testTournament.getImageUrl()).isEqualTo(UPDATED_IMAGE_URL);
        assertThat(testTournament.getLatitude()).isEqualTo(UPDATED_LATITUDE);
        assertThat(testTournament.getLongitude()).isEqualTo(UPDATED_LONGITUDE);
        assertThat(testTournament.getType()).isEqualTo(UPDATED_TYPE);

        // Validate the Tournament in Elasticsearch
        verify(mockTournamentSearchRepository, times(1)).save(testTournament);
    }

    @Test
    @Transactional
    public void updateNonExistingTournament() throws Exception {
        int databaseSizeBeforeUpdate = tournamentRepository.findAll().size();

        // Create the Tournament

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTournamentMockMvc.perform(put("/api/tournaments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tournament)))
            .andExpect(status().isBadRequest());

        // Validate the Tournament in the database
        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Tournament in Elasticsearch
        verify(mockTournamentSearchRepository, times(0)).save(tournament);
    }

    @Test
    @Transactional
    public void deleteTournament() throws Exception {
        // Initialize the database
        tournamentService.save(tournament);

        int databaseSizeBeforeDelete = tournamentRepository.findAll().size();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(tournament.getUser().getLogin(),
            tournament.getUser().getPassword()));
        SecurityContextHolder.setContext(securityContext);
        // Delete the tournament
        restTournamentMockMvc.perform(delete("/api/tournaments/{id}", tournament.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Tournament> tournamentList = tournamentRepository.findAll();
        assertThat(tournamentList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Tournament in Elasticsearch
        verify(mockTournamentSearchRepository, times(1)).deleteById(tournament.getId());
    }

    @Test
    @Transactional
    public void searchTournament() throws Exception {
        // Initialize the database
        tournamentService.save(tournament);
        when(mockTournamentSearchRepository.search(queryStringQuery("id:" + tournament.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(tournament), PageRequest.of(0, 1), 1));
        // Search the tournament
        restTournamentMockMvc.perform(get("/api/_search/tournaments?query=id:" + tournament.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tournament.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].meetingDate").value(hasItem(DEFAULT_MEETING_DATE.toString())))
            .andExpect(jsonPath("$.[*].meetingPoint").value(hasItem(DEFAULT_MEETING_POINT)))
            .andExpect(jsonPath("$.[*].city").value(hasItem(DEFAULT_CITY)))
            .andExpect(jsonPath("$.[*].price").value(hasItem(DEFAULT_PRICE)))
            .andExpect(jsonPath("$.[*].playerSize").value(hasItem(DEFAULT_PLAYER_SIZE)))
            .andExpect(jsonPath("$.[*].rewards").value(hasItem(DEFAULT_REWARDS)))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(DEFAULT_LATITUDE.intValue())))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(DEFAULT_LONGITUDE.intValue())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tournament.class);
        Tournament tournament1 = new Tournament();
        tournament1.setId(1L);
        Tournament tournament2 = new Tournament();
        tournament2.setId(tournament1.getId());
        assertThat(tournament1).isEqualTo(tournament2);
        tournament2.setId(2L);
        assertThat(tournament1).isNotEqualTo(tournament2);
        tournament1.setId(null);
        assertThat(tournament1).isNotEqualTo(tournament2);
    }
}
