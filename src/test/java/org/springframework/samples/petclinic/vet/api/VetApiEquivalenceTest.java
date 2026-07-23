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
package org.springframework.samples.petclinic.vet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.api.dto.SpecialtyResponse;
import org.springframework.samples.petclinic.vet.api.dto.VetResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * CP-05: Verifies that {@code GET /api/v1/vets} returns data functionally equivalent to
 * what {@link VetRepository#findAll()} contains. Compares id, firstName, lastName and
 * specialties for every vet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VetApiEquivalenceTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private VetRepository vetRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void apiResponseShouldBeEquivalentToRepositoryData() throws Exception {
		List<Vet> repositoryVets = List.copyOf(vetRepository.findAll());

		MvcResult result = mockMvc
			.perform(get("/api/v1/vets").accept(MediaType.APPLICATION_JSON)
				.with(jwt().authorities(new SimpleGrantedAuthority("VETS_READ"))))
			.andExpect(status().isOk())
			.andReturn();

		List<VetResponse> apiVets = objectMapper.readValue(result.getResponse().getContentAsByteArray(),
				new TypeReference<>() {
				});

		assertThat(apiVets).as("count must match repository").hasSize(repositoryVets.size());

		// Build independent expected list from repository (without reusing
		// VetResponse.from)
		List<VetResponse> expected = repositoryVets.stream().map(this::mapIndependently).toList();

		assertThat(apiVets).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
	}

	/**
	 * Independent mapping that does NOT reuse {@code VetResponse.from} — the expected
	 * values are built directly from the domain model so the test is not circular.
	 */
	private VetResponse mapIndependently(Vet vet) {
		List<SpecialtyResponse> specialties = vet.getSpecialties()
			.stream()
			.sorted(Comparator.comparing(s -> s.getId()))
			.map(s -> new SpecialtyResponse(s.getId(), s.getName()))
			.toList();
		return new VetResponse(vet.getId(), vet.getFirstName(), vet.getLastName(), specialties);
	}

}
