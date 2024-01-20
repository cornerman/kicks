package scala.annotation

// Somehow needed to be compatible with the code we are calling
// Otherwise we compile errors
class nowarn(@unused msg: String) extends StaticAnnotation
