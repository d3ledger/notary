set(_SOCI_REQUIRED_VARS SOCI_INCLUDE_DIR SOCI_LIBRARY SOCI_postgresql_PLUGIN)

add_library(SOCI::core UNKNOWN IMPORTED)
add_library(SOCI::postgresql UNKNOWN IMPORTED)

find_path(
    SOCI_INCLUDE_DIR soci.h
    PATH_SUFFIXES "" "soci"
    DOC "Soci (http://soci.sourceforge.net) include directory")
mark_as_advanced(SOCI_INCLUDE_DIR)
get_filename_component(_SOCI_INCLUDE_PARENT_DIR ${SOCI_INCLUDE_DIR} DIRECTORY)
set(SOCI_INCLUDE_DIRS ${SOCI_INCLUDE_DIR} ${_SOCI_INCLUDE_PARENT_DIR})
mark_as_advanced(SOCI_INCLUDE_DIRS)

find_library(
    SOCI_LIBRARY
    NAMES soci_core
    HINTS ${SOCI_INCLUDE_DIR}/..
    PATH_SUFFIXES lib${LIB_SUFFIX})
mark_as_advanced(SOCI_LIBRARY)

find_library(
    SOCI_postgresql_PLUGIN
    NAMES soci_postgresql
    HINTS ${SOCI_INCLUDE_DIR}/..
    PATH_SUFFIXES lib${LIB_SUFFIX})
mark_as_advanced(SOCI_postgresql_PLUGIN)

get_filename_component(SOCI_LIBRARY_DIR ${SOCI_LIBRARY} PATH)
mark_as_advanced(SOCI_LIBRARY_DIR)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(soci DEFAULT_MSG ${_SOCI_REQUIRED_VARS})

set(URL https://github.com/SOCI/soci)
set(VERSION 111b50af8c3876ea392367640b4bd83b4f903ab8) # 3.2.3
set_target_description(soci "The C++ Database Access Library" ${URL} ${VERSION})

if (NOT soci_FOUND)
  externalproject_add(soci_soci
      GIT_REPOSITORY  ${URL}
      GIT_TAG         ${VERSION}
      CONFIGURE_COMMAND ${CMAKE_COMMAND}
                      -G${CMAKE_GENERATOR}
                      -H${EP_PREFIX}/src/soci_soci/src
                      -B${EP_PREFIX}/src/soci_soci-build
                      -DCMAKE_INCLUDE_PATH=${pq_INCLUDE_DIR}
                      -DCMAKE_LIBRARY_PATH=${pq_INCLUDE_DIR}
                      -DCMAKE_PROGRAM_PATH=${pg_config_EXECUTABLE_DIR}
                      -DCMAKE_CXX_FLAGS=-I${postgres_INCLUDE_DIR}
                      -DCMAKE_INSTALL_PREFIX=${EP_PREFIX}
                      -DWITH_BOOST=ON
                      -DWITH_DB2=OFF
                      -DWITH_FIREBIRD=OFF
                      -DWITH_MYSQL=OFF
                      -DWITH_ODBC=OFF
                      -DWITH_ORACLE=OFF
                      -DWITH_POSTGRESQL=ON
                      -DWITH_SQLITE3=OFF
      BUILD_BYPRODUCTS ${EP_PREFIX}/src/soci_soci-build/lib/${CMAKE_STATIC_LIBRARY_PREFIX}soci_core${CMAKE_STATIC_LIBRARY_SUFFIX}
                       ${EP_PREFIX}/src/soci_soci-build/lib/${CMAKE_STATIC_LIBRARY_PREFIX}soci_postgresql${CMAKE_STATIC_LIBRARY_SUFFIX}
      TEST_COMMAND "" # remove test step
      UPDATE_COMMAND "" # remove update step
      )
  externalproject_get_property(soci_soci binary_dir)
  set(SOCI_INCLUDE_DIRS ${EP_PREFIX}/include ${EP_PREFIX}/include/soci)
  set(SOCI_LIBRARY ${binary_dir}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}soci_core${CMAKE_STATIC_LIBRARY_SUFFIX})
  set(SOCI_postgresql_PLUGIN ${binary_dir}/lib/${CMAKE_STATIC_LIBRARY_PREFIX}soci_postgresql${CMAKE_STATIC_LIBRARY_SUFFIX})
  file(MAKE_DIRECTORY ${EP_PREFIX}/include/soci)

  add_dependencies(soci_soci pq)
  add_dependencies(SOCI::core soci_soci)
  add_dependencies(SOCI::postgresql soci_soci)
endif ()

set_target_properties(SOCI::core PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${SOCI_INCLUDE_DIRS}"
    IMPORTED_LOCATION "${SOCI_LIBRARY}"
    INTERFACE_LINK_LIBRARIES dl
    )

set_target_properties(SOCI::postgresql PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${SOCI_INCLUDE_DIRS}"
    IMPORTED_LOCATION "${SOCI_postgresql_PLUGIN}"
    INTERFACE_LINK_LIBRARIES pq
    )

if(ENABLE_LIBS_PACKAGING)
  add_install_step_for_lib(${SOCI_LIBRARY})
  add_install_step_for_lib(${SOCI_postgresql_PLUGIN})
endif()
