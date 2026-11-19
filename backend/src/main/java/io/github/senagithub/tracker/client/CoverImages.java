package io.github.senagithub.tracker.client;

import java.util.List;

/**
 * The cover photos bundled with the frontend (frontend/public/images/covers).
 * Clients may only point at one of these, so a cover can never be a broken or low-quality image.
 */
public final class CoverImages {

    public static final String INFINITY_POOL = "/images/covers/infinity-pool.jpg";
    public static final String CAPPADOCIA_BALLOON = "/images/covers/cappadocia-balloon.jpg";
    public static final String PALM_POOL = "/images/covers/palm-pool.jpg";
    public static final String SPA_POOL = "/images/covers/spa-pool.jpg";
    public static final String ANTALYA_CLIFFS = "/images/covers/antalya-cliffs.jpg";
    public static final String SEASIDE_YOGA = "/images/covers/seaside-yoga.jpg";
    public static final String OLIVE_TABLE = "/images/covers/olive-table.jpg";

    public static final List<String> ALL = List.of(
            INFINITY_POOL, CAPPADOCIA_BALLOON, PALM_POOL, SPA_POOL, ANTALYA_CLIFFS, SEASIDE_YOGA, OLIVE_TABLE);

    private CoverImages() {
    }
}
