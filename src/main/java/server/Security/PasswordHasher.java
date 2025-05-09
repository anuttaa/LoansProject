package server.Security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean verify(String candidate, String hashed) {
        return BCrypt.checkpw(candidate, hashed);
    }
}
