package com.payanam.rail.dto.station;

import com.payanam.rail.domain.model.ArrivalType;
import jakarta.validation.constraints.NotBlank;

/** Create an arrival request. The type is optional: by default it follows the train's declared priority. */
public record ArrivalRequestBody(@NotBlank(message = "Choose a train") String trainNo, ArrivalType type) {
}
