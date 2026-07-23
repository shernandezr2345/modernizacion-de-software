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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.vet.service.VetQueryService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link VetsApiController} covering CP-01 through CP-07.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VetsApiControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private VetQueryService vetQueryService;

	@Test
	void shouldReturn200WithVetsReadAuthority() throws Exception {
		mockMvc
			.perform(get("/api/v1/vets").accept(MediaType.APPLICATION_JSON)
				.with(jwt().authorities(new SimpleGrantedAuthority("VETS_READ"))))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].id").exists())
			.andExpect(jsonPath("$[0].firstName").exists())
			.andExpect(jsonPath("$[0].lastName").exists())
			.andExpect(jsonPath("$[0].specialties").isArray());
	}

	@Test
	void shouldReturn401WithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/vets")).andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturn401WithInvalidToken() throws Exception {
		mockMvc.perform(get("/api/v1/vets").header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.valid.jwt"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturn403WithoutRequiredAuthority() throws Exception {
		mockMvc.perform(get("/api/v1/vets").with(jwt().authorities(new SimpleGrantedAuthority("OTHER_AUTHORITY"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void shouldReturn403WithEmptyAuthorities() throws Exception {
		mockMvc.perform(get("/api/v1/vets").with(jwt())).andExpect(status().isForbidden());
	}

	@Test
	void shouldKeepLegacyVetsEndpointAccessible() throws Exception {
		mockMvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
	}

	@Test
	void shouldKeepLegacyVetsHtmlEndpointAccessible() throws Exception {
		mockMvc.perform(get("/vets.html?page=1")).andExpect(status().isOk());
	}

	@Test
	void rejectedRequestsShouldNotInvokeVetQueryService() throws Exception {
		mockMvc.perform(get("/api/v1/vets")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/vets").with(jwt().authorities(new SimpleGrantedAuthority("OTHER_AUTHORITY"))))
			.andExpect(status().isForbidden());
		verifyNoInteractions(vetQueryService);
	}

}
