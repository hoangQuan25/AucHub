FROM quay.io/keycloak/keycloak:26.2.0

# Switch to root user to install packages
USER root

# Try to install curl using dnf
# If dnf fails, the next common one for RHEL-based systems would be yum,
# but dnf is more probable for a modern image.
RUN dnf install -y curl && \
    dnf clean all
# If the above dnf command fails with "dnf: command not found",
# you could then try replacing it with:
# RUN yum install -y curl && \
#     yum clean all

# Switch back to the default Keycloak user
USER keycloak
