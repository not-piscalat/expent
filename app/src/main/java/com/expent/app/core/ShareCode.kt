package com.expent.app.core

import kotlin.random.Random

/**
 * Short codes used to link a shared debt between two accounts. The partner
 * types the code into the Join flow and their device looks the debt up by it.
 *
 * The alphabet deliberately skips look-alike characters (0/O, 1/I) so codes
 * are easy to read aloud or retype from a screenshot.
 */
object ShareCode {

    const val CODE_LENGTH = 6

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** A fresh code, e.g. "K7M2QX". Pass a seeded [Random] in tests. */
    fun generate(length: Int = CODE_LENGTH, random: Random = Random.Default): String =
        buildString(length) {
            repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }

    /**
     * A code that does not collide with any code already in use locally.
     * Collisions across accounts are handled by the join lookup (limit 1) and
     * are effectively impossible at family-app scale (36^6 combinations).
     */
    fun generateUnique(existing: Set<String>, random: Random = Random.Default): String {
        var code = generate(random = random)
        while (code in existing) code = generate(random = random)
        return code
    }

    /**
     * Normalizes whatever the user typed: uppercase, keeps only letters and
     * digits, trims to the canonical length. "k7m 2qx" and "k7m2qx!" both
     * become "K7M2QX".
     */
    fun normalize(input: String, length: Int = CODE_LENGTH): String =
        input.uppercase().filter { it.isLetterOrDigit() }.take(length)
}
