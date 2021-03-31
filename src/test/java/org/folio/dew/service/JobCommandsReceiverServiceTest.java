package org.folio.dew.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.InMemoryAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;

@SpringBootTest(
    classes = {JobCommandsReceiverService.class, InMemoryAcknowledgementRepository.class})
class JobCommandsReceiverServiceTest {

  @Autowired private JobCommandsReceiverService service;
  @Autowired private InMemoryAcknowledgementRepository repository;
  @MockBean private ExportJobManager exportJobManager;
  @MockBean private MinIOObjectStorageRepository minIOObjectStorageRepository;
  @Mock private Acknowledgment acknowledgment;

  @Test
  @DisplayName("Start job by kafka request")
  void startJobTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartJobRequest(id);

    service.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManager, times(1)).launchJob(any());

    final Acknowledgment savedAcknowledgment = repository.getAcknowledgement(id.toString());

    assertNotNull(savedAcknowledgment);
  }

  @Test
  @DisplayName("Delete files by kafka request")
  void deleteFilesTest() {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createDeleteJobRequest(id);

    service.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(acknowledgment, times(1)).acknowledge();
  }

  private JobCommand createStartJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.CIRCULATION_LOG.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.CIRCULATION_LOG);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter(""));
    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }

  private JobCommand createDeleteJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.DELETE);
    jobCommand.setId(id);
    jobCommand.setJobParameters(
        new JobParameters(
            Collections.singletonMap(
                JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter(""))));
    return jobCommand;
  }
}
