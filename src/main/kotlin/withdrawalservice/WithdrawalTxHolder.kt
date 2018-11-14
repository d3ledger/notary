package withdrawalservice

import io.reactivex.Observable

interface WithdrawalTxHolder<SourceTranscationDescription, TargetTranscationDescription> {

    fun store(
        sourceTranscationDescription: SourceTranscationDescription,
        targetTranscationDescription: TargetTranscationDescription
    )

    fun getTarget(sourceTranscationDescription: SourceTranscationDescription): TargetTranscationDescription

    fun removeEntry(sourceTranscationDescription: SourceTranscationDescription)

    fun getObservable(): Observable<Map.Entry<SourceTranscationDescription, TargetTranscationDescription>>
}
