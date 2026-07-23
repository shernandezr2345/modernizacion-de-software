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

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.vet.api.dto.VetResponse;
import org.springframework.samples.petclinic.vet.service.VetQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Versioned REST endpoint for veterinarian data. Protected by JWT; requires the
 * {@code VETS_READ} authority. Does not modify or replace {@code VetController}.
 */
@RestController
@RequestMapping(path = "/api/v1/vets", produces = MediaType.APPLICATION_JSON_VALUE)
public class VetsApiController {

	private final VetQueryService vetQueryService;

	public VetsApiController(VetQueryService vetQueryService) {
		this.vetQueryService = vetQueryService;
	}

	@GetMapping
	public List<VetResponse> list() {
		return vetQueryService.listAll();
	}

}
