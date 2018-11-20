package withdrawalservice

import io.reactivex.Observable

interface WithdrawalTxDAO<SourceTranscationDescription, TargetTranscationDescription> {

    fun put(
        sourceTranscationDescription: SourceTranscationDescription,
        targetTranscationDescription: TargetTranscationDescription
    )

    fun get(sourceTranscationDescription: SourceTranscationDescription): TargetTranscationDescription

    fun remove(sourceTranscationDescription: SourceTranscationDescription)

    fun getObservable(): Observable<Map.Entry<SourceTranscationDescription, TargetTranscationDescription>>
}
