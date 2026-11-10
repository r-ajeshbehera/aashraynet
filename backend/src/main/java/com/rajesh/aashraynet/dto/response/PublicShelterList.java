package com.rajesh.aashraynet.dto.response;

import java.time.Instant;
import java.util.List;

public record PublicShelterList(String dataNotice, Instant generatedAt, List<PublicShelterResponse> shelters) { }
