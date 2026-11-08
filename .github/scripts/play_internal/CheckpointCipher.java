import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Authenticated encryption uses only the JDK. The key is NEVER an argument.
class CheckpointCipher {
    public static void main(String[] args) throws Exception {
        var encodedKey = System.getenv("PLAY_CHECKPOINT_KEY");
        if (encodedKey == null) throw new SecurityException("Checkpoint key missing");
        byte[] key = Base64.getDecoder().decode(encodedKey);
        if (key.length != 32) throw new SecurityException("Checkpoint key must be 256 bits");
        var input = Path.of(args[1]);
        if (Files.size(input) > 150_000_000) throw new SecurityException("Checkpoint too large");
        byte[] data = Files.readAllBytes(input);
        byte[] magic = "TYLCP001".getBytes(StandardCharsets.US_ASCII);
        byte[] nonce = new byte[12];
        boolean encrypt = args[0].equals("encrypt");
        if (encrypt) {
            new SecureRandom().nextBytes(nonce);
        } else {
            if (!args[0].equals("decrypt") || data.length < 36 || !Arrays.equals(magic, Arrays.copyOf(data, 8)))
                throw new SecurityException("Unknown checkpoint container");
            nonce = Arrays.copyOfRange(data, 8, 20);
        }
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(magic);
        // No plaintext is written until authentication of the entire container.
        byte[] result = cipher.doFinal(encrypt ? data : Arrays.copyOfRange(data, 20, data.length));
        if (encrypt) result = ByteBuffer.allocate(20 + result.length).put(magic).put(nonce).put(result).array();
        Files.write(Path.of(args[2]), result);
        Arrays.fill(key, (byte) 0);
        System.out.println("Authenticated checkpoint operation passed");
    }
}
