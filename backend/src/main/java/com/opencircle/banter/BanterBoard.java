package com.opencircle.banter;

import java.util.List;

record BanterBoard(List<BanterView> items, int page, int size, long totalElements, int totalPages) {
}
