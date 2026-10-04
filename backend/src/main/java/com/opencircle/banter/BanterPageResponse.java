package com.opencircle.banter;

import java.util.List;

record BanterPageResponse(List<BanterResponse> items, int page, int size, long totalElements, int totalPages) {
}
