DESCRIPTION="mbed-cloud-client-example"

TOOLCHAIN = "POKY-GLIBC"
LICENSE = "Apache-2.0"
LICENSE_MD5SUM = "4336ad26bb93846e47581adc44c4514d"
SOURCE_REPOSITORY = "git://git@github.com/PelionIoT/mbed-cloud-client-example.git"
SOURCE_BRANCH = "master"
SRCREV = "f20a0c0eaf91f038598d0ad333d6c79f713cf00e"
APP_NAME = "mbed-cloud-client-example"

LIC_FILES_CHKSUM = "file://${WORKDIR}/git/${APP_NAME}/mbed-cloud-client/LICENSE;md5=${LICENSE_MD5SUM}"

# Patches for quilt goes to files directory
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
CFLAGS += "-D_FILE_OFFSET_BITS=64"

SRC_URI = "${SOURCE_REPOSITORY};branch=${SOURCE_BRANCH};protocol=ssh;name=${APP_NAME};destsuffix=git/${APP_NAME}; \
    file://yocto-toolchain.cmake \
    file://fota_update_activate.sh \
    file://fota_install_callback.c \
    file://CloudClientExample.sh"

DEPENDS = " glibc"
RDEPENDS_${PN} = " libstdc++ libgcc"

# This needed e.g. for calling pip module binaries like mbed-cli commands. If local
# environment is using some other directory then override binlocaldir.
binlocaldir ?= "${exec_prefix}/local/bin"

# Installed packages
PACKAGES = "${PN}-dbg ${PN}"
FILES_${PN} += "/opt \
                /opt/arm \
                /opt/arm/mbedCloudClientExample.elf"
FILES_${PN}-dbg += "/opt/arm/.debug \
                    /usr/src/debug/mbed-cloud-client"

TARGET = "Yocto_Generic_YoctoLinux_mbedtls"

# Allowed [Debug|Release]
RELEASE_TYPE="Debug"

# Build build with -O0 as FORTIFY_SOURCE requires -O0 optimization
DEBUG_BUILD="1"

inherit cmake

do_setup_pal_env() {
    echo "Setup pal env"
    CUR_DIR=$(pwd)
    cd "${WORKDIR}/git/${APP_NAME}"
    # Clean the old build directory
    rm -rf "__${TARGET}"
    export PATH=$PATH:${binlocaldir}
    SSH_AUTH_SOCK=${SSH_AUTH_SOCK} python -m mbed deploy --protocol ssh
    SSH_AUTH_SOCK=${SSH_AUTH_SOCK} python ./pal-platform/pal-platform.py -v deploy --target="${TARGET}" generate
    cd ${CUR_DIR}
}

addtask setup_pal_env after do_unpack before do_patch

do_configure() {
    CUR_DIR=$(pwd)
    cd "${WORKDIR}/git/${APP_NAME}/__${TARGET}"

    if [ -e "${MBED_CLOUD_IDENTITY_CERT_FILE}" ]; then
        cp ${MBED_CLOUD_IDENTITY_CERT_FILE} "${WORKDIR}/git/${APP_NAME}/mbed_cloud_dev_credentials.c"
    else
        # Not set.
        echo "ERROR certification file does not set !!!"
        exit 1
    fi

    if [ -e "${MBED_UPDATE_RESOURCE_FILE}" ]; then
        cp ${MBED_UPDATE_RESOURCE_FILE} "${WORKDIR}/git/${APP_NAME}/update_default_resources.c"
    fi

    cp "${WORKDIR}/yocto-toolchain.cmake" "${WORKDIR}/git/${APP_NAME}/pal-platform/Toolchain/${TOOLCHAIN}"
    if [ "${FOTA_ENABLE}" = "1" ]; then
        cp "${WORKDIR}/fota_install_callback.c" "${WORKDIR}/git/${APP_NAME}/fota_install_callback.c"
    fi

    # Set cmake extra defines
    EXTRA_DEFINES=""
    if [ "${RESET_STORAGE}" = "1" ]; then
        echo "Define RESET_STORAGE for cmake."
        EXTRA_DEFINES="-DRESET_STORAGE=ON"
    fi
    
    if [ "${FOTA_ENABLE}" = "1" ]; then
        echo "Define FOTA_ENABLE for cmake."
        EXTRA_DEFINES="$EXTRA_DEFINES -DFOTA_ENABLE=ON"         
    fi

    if [ "${FOTA_TRACE}" = "1" ]; then
        echo "Define FOTA_TRACE for cmake."
        EXTRA_DEFINES="$EXTRA_DEFINES -DFOTA_TRACE=ON"         
    fi    

    echo ${EXTRA_DEFINES}
    
    YOCTO_DIR=${TOPDIR} cmake -G "Unix Makefiles" ${EXTRA_DEFINES} -DCMAKE_BUILD_TYPE="${RELEASE_TYPE}" -DCMAKE_TOOLCHAIN_FILE="./../pal-platform/Toolchain/${TOOLCHAIN}/${TOOLCHAIN}.cmake" -DEXTERNAL_DEFINE_FILE="./../define-rpi3-yocto.txt"
    cd ${CUR_DIR}
}

do_compile() {
    CUR_DIR=$(pwd)
    cd "${WORKDIR}/git/${APP_NAME}/__${TARGET}"
    make mbedCloudClientExample.elf
    cd ${CUR_DIR}
}

do_install() {
    install -d "${D}/opt/arm"
    install "${WORKDIR}/git/${APP_NAME}/__${TARGET}/${RELEASE_TYPE}/mbedCloudClientExample.elf" "${D}/opt/arm"

    install -d "${D}${sysconfdir}/init.d"
    install "${WORKDIR}/CloudClientExample.sh" "${D}${sysconfdir}/init.d"

    if [ "${FOTA_ENABLE}" = "1" ]; then
        # Install fota-scripts
        install -m 555 "${WORKDIR}/fota_update_activate.sh"      "${D}/opt/arm"
    fi
}

INITSCRIPT_PACKAGES = "${PN}"
INITSCRIPT_NAME = "CloudClientExample.sh"
INITSCRIPT_PARAMS = "defaults 99"

inherit update-rc.d
