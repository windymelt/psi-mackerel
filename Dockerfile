FROM sbtscala/scala-sbt:openjdk-17.0.2_1.8.0_3.2.1 as build
RUN apt update -y && apt -y install clang llvm-13 llvm-13-tools libcurl4-openssl-dev libz1 zlib1g upx
RUN mkdir /app
WORKDIR /app
COPY build.sbt /app/build.sbt
COPY src/ /app/src/
COPY project/ /app/project/
RUN sbt nativeLink
RUN upx /app/target/scala-3.2.1/psi-mackerel-out
RUN chmod u+x /app/target/scala-3.2.1/psi-mackerel-out
RUN mv /app/target/scala-3.2.1/psi-mackerel-out /app/psi-mackerel

FROM gcr.io/distroless/cc
COPY --from=build /app/psi-mackerel /
COPY --from=build /usr/lib/x86_64-linux-gnu/libcurl* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libnghttp2* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libidn2* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/librtmp* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libssh2* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libpsl* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libgssapi_krb5* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libldap_r* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/liblber* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlidec* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libbrotlicommon* /usr/lib/x86_64-linux-gnu/
COPY --from=build /lib/x86_64-linux-gnu/libz* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libunistring* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libgnutls* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libhogweed* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libnettle* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libgmp* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libgcrypt* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libkrb5* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libk5crypto* /usr/lib/x86_64-linux-gnu/
COPY --from=build /lib/x86_64-linux-gnu/libcom_err* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libsasl2* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libp11-kit* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libtasn1* /usr/lib/x86_64-linux-gnu/
COPY --from=build /lib/x86_64-linux-gnu/libgpg-error* /usr/lib/x86_64-linux-gnu/
COPY --from=build /lib/x86_64-linux-gnu/libkeyutils* /usr/lib/x86_64-linux-gnu/
COPY --from=build /usr/lib/x86_64-linux-gnu/libffi* /usr/lib/x86_64-linux-gnu/
CMD ["/psi-mackerel"]
ENTRYPOINT ["/psi-mackerel"]
