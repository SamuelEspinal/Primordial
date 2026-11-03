/** Names are hashes of identity, never draws from the biological random generator. */
public final class CreatureNames {
    private static final String[] FIRST={"Luma","Moss","Pip","Sora","Fern","Nori","Clover","Wren","Aster","Iris","Taro","Briar","Ember","Nim","Sage","Koa","Mira","Orin","Fable","Rune","Willow","Cedar","Lark","Nova","Reed","Sol","Echo","Tula","Oona","Finch","Lyra","Ash"};
    private static final String[] A={"Amber","Willow","Moon","Moss","Cedar","Silver","River","Dawn","Fern","Coral","Meadow","Star","Rain","Birch","Sun","Cloud","Quiet","Wild","Copper","Jade","Ocean","Mist","Snow","Bloom"};
    private static final String[] B={"brook","crest","grove","vale","stone","leaf","song","tide","glade","bloom","spring","wood","haven","wing","water","fern","meadow","shore","field","bough","root","dew","reed","hollow"};
    private static long hash(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    private static String identityWord(int id){StringBuilder b=new StringBuilder();long n=Math.max(1,id);do{n--;b.append(FIRST[(int)(n%FIRST.length)]);n/=FIRST.length;}while(n>0);return b.toString();}
    public static String name(long seed,int id){long h=hash(seed+id*0x9e3779b97f4a7c15L);return FIRST[Math.floorMod(h,FIRST.length)]+" "+identityWord(id);}
    public static String clan(long seed,int root){long h=hash(seed+root*0x632be59bd9b4e019L);return A[Math.floorMod(h,A.length)]+B[Math.floorMod(h>>>19,B.length)]+" "+identityWord(root)+" Clan";}
    private CreatureNames(){}
}
