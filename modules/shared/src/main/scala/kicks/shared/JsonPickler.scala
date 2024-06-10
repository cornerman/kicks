package kicks.shared

// configuration of json serialization, see: https: //com-lihaoyi.github.io/upickle/
object JsonPickler extends upickle.AttributeTagged with chameleon.ext.upickle {
  // map Option[T] to null when the option is None
  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None    => null.asInstanceOf[T]
      case Some(x) => x
    }
  override implicit def OptionReader[T: Reader]: Reader[Option[T]] =
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))) {
      override def visitNull(index: Int) = None
    }
}
