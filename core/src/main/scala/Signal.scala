package reinventation_core

import chisel3._

object BoolSignal{
	val T=true.B
	val F=false.B
}

object ALUASourceSignal{
	val width=2.W
	val A_0=0.U(width)//zero add something->copy b
	val A_REGA=1.U(width)
	val A_PC=2.U(width)//aiupc,etc
}

object ALUBSourceSignal{
	val width=2.W
	val B_0=0.U(width)//something add zero->copy a
	val B_REGB=1.U(width)
	val B_IMM=2.U(width)
}

object ImmTypeSignal{
	val width=3.W
	val IMM_S=0.U(width)
	val IMM_B=1.U(width)
	val IMM_U=2.U(width)
	val IMM_J=3.U(width)
	val IMM_I=4.U(width)
}