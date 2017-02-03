/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class test_repository_DomainRepository */

#ifndef _Included_test_repository_DomainRepository
#define _Included_test_repository_DomainRepository
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     test_repository_DomainRepository
 * Method:    accountUpdateQuantity
 * Signature: (Ljava/lang/String;Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_test_repository_DomainRepository_accountUpdateQuantity
  (JNIEnv *, jclass, jstring, jstring, jlong);

/*
 * Class:     test_repository_DomainRepository
 * Method:    accountAttach
 * Signature: (Ljava/lang/String;Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_test_repository_DomainRepository_accountAttach
  (JNIEnv *, jclass, jstring, jstring, jlong);

/*
 * Class:     test_repository_DomainRepository
 * Method:    accountFindByUuid
 * Signature: (Ljava/lang/String;)Ljava/util/HashMap;
 */
JNIEXPORT jobject JNICALL Java_test_repository_DomainRepository_accountFindByUuid
  (JNIEnv *, jclass, jstring);

/*
 * Class:     test_repository_DomainRepository
 * Method:    accountAdd
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_test_repository_DomainRepository_accountAdd
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     test_repository_DomainRepository
 * Method:    assetAdd
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_test_repository_DomainRepository_assetAdd
  (JNIEnv *, jclass, jstring, jstring, jstring);

/*
 * Class:     test_repository_DomainRepository
 * Method:    assetFindByUuid
 * Signature: (Ljava/lang/String;)Ljava/util/HashMap;
 */
JNIEXPORT jobject JNICALL Java_test_repository_DomainRepository_assetFindByUuid
  (JNIEnv *, jclass, jstring);

/*
 * Class:     test_repository_DomainRepository
 * Method:    assetUpdate
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_test_repository_DomainRepository_assetUpdate
  (JNIEnv *, jclass, jstring, jstring, jstring);

/*
 * Class:     test_repository_DomainRepository
 * Method:    assetRemove
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_test_repository_DomainRepository_assetRemove
  (JNIEnv *, jclass, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif
