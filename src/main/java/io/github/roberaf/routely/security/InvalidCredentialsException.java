package io.github.roberaf.routely.security;

/**
 * Thrown for both an unknown email and a wrong password: the login endpoint
 * must not leak which of the two failed.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid email or password");
	}
}
