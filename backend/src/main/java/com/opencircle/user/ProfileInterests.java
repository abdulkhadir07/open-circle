package com.opencircle.user;

import java.util.List;

public record ProfileInterests(String bio, List<String> interests) {

    public static final ProfileInterests EMPTY = new ProfileInterests(null, List.of());
}
