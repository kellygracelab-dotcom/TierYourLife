import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.jar.JarFile;

// JarFile verifies each entry only AFTER its bytes have been completely read.
// jarsigner's exit code alone also accepts an entirely unsigned JAR.
class VerifyBundle {
    public static void main(String[] args) throws Exception {
        var expected = args[1]; // "unsigned", or the pinned public certificate digest
        int payloads = 0;
        try (var jar = new JarFile(Path.of(args[0]).toFile(), true)) {
            var entries = jar.entries();
            var seen = new java.util.HashSet<String>();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!seen.add(entry.getName())) throw new SecurityException("Duplicate entry");
                if (entry.isDirectory()) continue;
                try (var input = jar.getInputStream(entry)) { input.transferTo(java.io.OutputStream.nullOutputStream()); }
                var name = entry.getName().toUpperCase(Locale.ROOT);
                boolean signature = name.matches("META-INF/(MANIFEST\\.MF|[^/]+\\.(SF|RSA|DSA|EC)|SIG-[^/]+)");
                var signers = entry.getCodeSigners();
                if (expected.equals("unsigned")) {
                    if (signers != null || signature) throw new SecurityException("Not unsigned");
                } else if (!signature) {
                    if (signers == null || signers.length != 1) throw new SecurityException("Unsigned or multi-signed entry");
                    var cert = signers[0].getSignerCertPath().getCertificates().get(0);
                    var digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
                    if (!digest.equalsIgnoreCase(expected)) throw new SecurityException("Unexpected signer");
                    payloads++;
                }
            }
        }
        if (!expected.equals("unsigned") && payloads == 0) throw new SecurityException("Empty bundle");
        System.out.println("Bundle signature policy passed");
    }
}
