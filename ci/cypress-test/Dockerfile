#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# Align the major version of cypress with version of cypress in eric-oss-auto-config-consistency-ui/package.json
FROM cypress/included:12.0.0

# there is a built-in user "node" that comes from the very base Docker Node image
# we are going to recreate this user and give it _same id_ as external user
# that is going to run this container.
ARG USER_ID=7128776
ARG GROUP_ID=64000

RUN groupadd -g ${GROUP_ID} eoadm100
# do not log creating new user, otherwise there could be a lot of messages
RUN useradd -r --no-log-init -u ${USER_ID} -g eoadm100 eoadm100
RUN install -d -m 0755 -o eoadm100 -g eoadm100 /home/eoadm100

# move test runner binary folder to the non-root's user home directory
RUN mv /root/.cache /home/eoadm100/.cache
# make sure cypress looks in the right place
ENV CYPRESS_CACHE_FOLDER=/home/eoadm100/.cache/Cypress

USER eoadm100