package org.folio.dew;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.folio.des.service.JobExecutionService;
import org.folio.des.service.JobUpdatesService;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.util.SocketUtils;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
      "minio.endpoint=http://${embedded.minio.host}:${embedded.minio.port}/",
    })
@EmbeddedKafka(
    topics = {
      JobUpdatesService.DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME,
      JobExecutionService.DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME
    })
@EnableKafka
@EnableBatchProcessing
public abstract class BaseBatchTest {
  protected static final String TOKEN =
      "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  public static final int WIRE_MOCK_PORT = SocketUtils.findAvailableTcpPort();
  public static WireMockServer wireMockServer;

  @Autowired private FolioModuleMetadata folioModuleMetadata;
  @Autowired protected JobLauncher jobLauncher;
  @Autowired protected JobRepository jobRepository;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected MinIOObjectStorageRepository minIOObjectStorageRepository;

  @Value("${spring.application.name}")
  protected String springApplicationName;

  @BeforeAll
  static void beforeAll() {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();
  }

  @BeforeEach
  void setUp() {
    Map<String, Collection<String>> okapiHeaders = new LinkedHashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    okapiHeaders.put(XOkapiHeaders.TOKEN, List.of(TOKEN));
    okapiHeaders.put(XOkapiHeaders.URL, List.of(wireMockServer.baseUrl()));
    var defaultFolioExecutionContext =
        new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
        defaultFolioExecutionContext);

    minIOObjectStorageRepository.createBucketIfNotExists();
  }

  protected JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }
}
