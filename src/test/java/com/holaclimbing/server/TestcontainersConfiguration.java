package com.holaclimbing.server;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
	}

	/**
	 * 테스트용 GCS Storage. 실제 버킷 대신 고정 Signed URL을 반환해
	 * GcsStorageService가 인증 없이도 동작하도록 한다.
	 */
	@Bean
	Storage gcsStorage() throws Exception {
		Storage storage = Mockito.mock(Storage.class);
		URL signedUrl = URI.create("https://storage.googleapis.com/hola-test/signed-url").toURL();
		Mockito.when(storage.signUrl(
						Mockito.any(BlobInfo.class), Mockito.anyLong(), Mockito.any(TimeUnit.class),
						Mockito.any(Storage.SignUrlOption[].class)))
				.thenReturn(signedUrl);
		return storage;
	}

}
