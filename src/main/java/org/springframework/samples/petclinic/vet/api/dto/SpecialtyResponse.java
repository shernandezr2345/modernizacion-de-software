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
package org.springframework.samples.petclinic.vet.api.dto;

import java.util.Objects;

import org.springframework.samples.petclinic.vet.Specialty;

/**
 * Read-only DTO representing a veterinarian specialty. Exposes only id and name; never
 * exposes the JPA entity.
 */
public record SpecialtyResponse(Integer id, String name) {

	public static SpecialtyResponse from(Specialty specialty) {
		Objects.requireNonNull(specialty, "specialty must not be null");
		return new SpecialtyResponse(specialty.getId(), specialty.getName());
	}

}
