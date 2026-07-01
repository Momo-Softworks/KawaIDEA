;; Project-scoped Guix shell environment for KawaIDEA.
;;
;; Usage:
;;   guix shell          → drops into a shell with JDK 21 on PATH
;;   guix shell -- make  → run a make target without entering a shell
;;
;; Gradle's jvmToolchain(21) needs a *JDK* (not just JRE) visible to its
;; toolchain detection.  openjdk21 splits compiler tools into the `jdk` output,
;; so we request that output explicitly.

(use-modules (gnu packages java)
             (gnu packages bash)
             (gnu packages base))   ; which

(packages->manifest
 (list (list openjdk21 "jdk")  ; javac + java — satisfies jvmToolchain(21)
       bash                    ; ./gradlew shebang
       which))                 ; Gradle toolchain detection probes `which java`
