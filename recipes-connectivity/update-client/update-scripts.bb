DESCRIPTION="update-scripts"

LICENSE = "Apache-2.0"
LICENSE_MD5SUM = "4336ad26bb93846e47581adc44c4514d"
SOURCE_REPOSITORY = "git://git@github.com/ARMmbed/mbed-cloud-client.git"
SOURCE_BRANCH = "master"
SRCREV = "e03c516af9c9137b56d9c2620a293c79f1f867f8"
SCRIPT_DIR = "${WORKDIR}/git/update-client-hub/modules/pal-linux/scripts"

# Default Update scripts are for Raspberrypi3. Please override these for HW specific srcipts if required.
UPDATE_CMDLINE          ?= "${SCRIPT_DIR}/arm_update_cmdline.sh"
UPDATE_ACTIVATE         ?= "${SCRIPT_DIR}/yocto_rpi/arm_update_activate.sh"
UPDATE_ACTIVATE_DETAILS ?= "${SCRIPT_DIR}/yocto_rpi/arm_update_active_details.sh"
UPDATE_PREPARE         ?= "${SCRIPT_DIR}/yocto_rpi/arm_update_prepare.sh"
# To add an extra file into rootfs under /opt/arm (for example to test very large update binary sizes),
# do the following:
# 1. Uncomment the below EXTRA_FILES and set it to point to your testfile filename/path
# 2. Uncomment the "SRC_URI +=" -lines below
# 3. Uncomment the commented lines in do_install() -section
# EXTRA_FILES = "/home/user/testfile"

PACKAGE_ARCH = "${MACHINE_ARCH}"

LIC_FILES_CHKSUM = "file://${WORKDIR}/git/LICENSE;md5=${LICENSE_MD5SUM}"

# Patches for quilt goes to files directory
#FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI = "${SOURCE_REPOSITORY};branch=${SOURCE_BRANCH};protocol=ssh"
#SRC_URI += " \
#         file://${EXTRA_FILES} \
#"


# This needed e.g. for calling pip module binaries like mbed-cli commands. If local
# environment is using some other directory then override binlocaldir.
binlocaldir ?= "${exec_prefix}/local/bin"

# tar and bzip2 as runtime dependency
RDEPENDS_${PN} = "tar bzip2"

# Deploy for getting update-client-hub to working area
do_configure() {
    cd "${WORKDIR}/git"
    export PATH=$PATH:${binlocaldir}
    SSH_AUTH_SOCK=${SSH_AUTH_SOCK} python -m mbed deploy --protocol ssh
}

# Install update-scripts
do_install() {
    install -d "${D}/opt/arm"
    install -m 755 "${UPDATE_CMDLINE}"          "${D}/opt/arm"
    install -m 755 "${UPDATE_ACTIVATE}"         "${D}/opt/arm"
    install -m 755 "${UPDATE_ACTIVATE_DETAILS}" "${D}/opt/arm"
    install -m 755 "${UPDATE_PREPARE}"          "${D}/opt/arm"
#    if [ -e ${EXTRA_FILES} ]; then
#         install -m 0644 ${EXTRA_FILES}         "${D}/opt/arm"
#    fi
}

FILES_${PN} += "/opt \
                /opt/arm"
