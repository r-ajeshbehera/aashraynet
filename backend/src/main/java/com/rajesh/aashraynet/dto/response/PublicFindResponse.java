package com.rajesh.aashraynet.dto.response;

import java.util.List;

public record PublicFindResponse(List<PublicMatch> matches, String notice) { }
