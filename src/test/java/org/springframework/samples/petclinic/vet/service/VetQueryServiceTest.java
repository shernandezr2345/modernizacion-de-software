/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.api.dto.VetResponse;

/**
 * Unit tests for {@link VetQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class VetQueryServiceTest {

	@Mock
	private VetRepository repository;

	private VetQueryService service;

	@BeforeEach
	void setUp() {
		service = new VetQueryService(repository);
	}

	@Test
	void shouldReturnEmptyListWhenNoVets() {
		when(repository.findAll()).thenReturn(List.of());

		List<VetResponse> result = service.listAll();

		assertThat(result).isEmpty();
		verify(repository).findAll();
		verifyNoMoreInteractions(repository);
	}

	@Test
	void shouldMapVetFieldsToDto() {
		Vet vet = vet(1, "James", "Carter");
		when(repository.findAll()).thenReturn(List.of(vet));

		List<VetResponse> result = service.listAll();

		assertThat(result).hasSize(1);
		VetResponse dto = result.get(0);
		assertThat(dto.id()).isEqualTo(1);
		assertThat(dto.firstName()).isEqualTo("James");
		assertThat(dto.lastName()).isEqualTo("Carter");
		assertThat(dto.specialties()).isNotNull().isEmpty();
		verify(repository).findAll();
		verifyNoMoreInteractions(repository);
	}

	@Test
	void shouldMapSpecialtiesToDto() {
		Vet vet = vet(2, "Helen", "Leary");
		Specialty radiology = specialty(1, "radiology");
		vet.addSpecialty(radiology);
		when(repository.findAll()).thenReturn(List.of(vet));

		List<VetResponse> result = service.listAll();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).specialties()).hasSize(1);
		assertThat(result.get(0).specialties().get(0).id()).isEqualTo(1);
		assertThat(result.get(0).specialties().get(0).name()).isEqualTo("radiology");
		verify(repository).findAll();
		verifyNoMoreInteractions(repository);
	}

	@Test
	void shouldReturnEmptySpecialtiesListWhenVetHasNone() {
		when(repository.findAll()).thenReturn(List.of(vet(3, "Linda", "Douglas")));

		List<VetResponse> result = service.listAll();

		assertThat(result.get(0).specialties()).isNotNull().isEmpty();
	}

	@Test
	void shouldDelegateExactlyOnceToRepository() {
		when(repository.findAll()).thenReturn(List.of());

		service.listAll();

		verify(repository).findAll();
		verifyNoMoreInteractions(repository);
	}

	@Test
	void resultShouldNotContainJpaEntities() {
		when(repository.findAll()).thenReturn(List.of(vet(1, "James", "Carter")));

		List<VetResponse> result = service.listAll();

		assertThat(result).allSatisfy(dto -> {
			assertThat(dto).isNotInstanceOf(Vet.class);
			assertThat(dto.specialties()).allSatisfy(s -> assertThat(s).isNotInstanceOf(Specialty.class));
		});
	}

	private static Vet vet(int id, String firstName, String lastName) {
		Vet vet = new Vet();
		vet.setId(id);
		vet.setFirstName(firstName);
		vet.setLastName(lastName);
		return vet;
	}

	private static Specialty specialty(int id, String name) {
		Specialty s = new Specialty();
		s.setId(id);
		s.setName(name);
		return s;
	}

}
