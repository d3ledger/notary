package withdrawalservice

import io.reactivex.Observable

interface WithdrawalTxDAO<SourceTranscationDescription, TargetTranscationDescription> {

    fun store(
        sourceTranscationDescription: SourceTranscationDescription,
        targetTranscationDescription: TargetTranscationDescription
    )

    fun getTarget(sourceTranscationDescription: SourceTranscationDescription): TargetTranscationDescription

    fun remove(sourceTranscationDescription: SourceTranscationDescription)

    fun getObservable(): Observable<Map.Entry<SourceTranscationDescription, TargetTranscationDescription>>
}
