package com.prideregion.flashpoints.layout;

import java.util.*;

public final class DungeonGenerator {
    public static final int WIDTH=100, LENGTH=76, HEIGHT=9, COLS=4, ROWS=3;
    private DungeonGenerator() {}

    public static DungeonLayout generate(long seed) {
        Random random=new Random(seed);
        int[][] centersX=new int[ROWS][COLS], centersZ=new int[ROWS][COLS];
        boolean[][] walk=new boolean[LENGTH][WIDTH];
        for(int z=0;z<ROWS;z++) for(int x=0;x<COLS;x++) {
            int cx=13+x*24+random.nextInt(5)-2;
            int cz=13+z*24+random.nextInt(5)-2;
            int rx=(x==3&&z==1)?10:7+random.nextInt(4);
            int rz=(x==3&&z==1)?10:7+random.nextInt(4);
            centersX[z][x]=cx; centersZ[z][x]=cz;
            int shape=random.nextInt(3);
            for(int dz=-rz;dz<=rz;dz++) for(int dx=-rx;dx<=rx;dx++) {
                double nx=Math.abs((double)dx/rx), nz=Math.abs((double)dz/rz);
                boolean carve=shape==0 ? nx*nx+nz*nz<=1
                    : shape==1 ? Math.max(nx,nz)+0.3*Math.min(nx,nz)<=1
                    : (Math.abs(dx)<=4||Math.abs(dz)<=4);
                int px=cx+dx,pz=cz+dz;
                if(carve && px>0 && pz>0 && px<WIDTH-1 && pz<LENGTH-1) walk[pz][px]=true;
            }
        }
        // Spanning tree on the room grid guarantees every room has a path to the entrance.
        boolean[] seen=new boolean[COLS*ROWS];
        ArrayDeque<Integer> stack=new ArrayDeque<>();
        stack.push(COLS); seen[COLS]=true;
        Set<Long> edges=new TreeSet<>();
        while(!stack.isEmpty()) {
            int here=stack.peek(),x=here%COLS,z=here/COLS;
            ArrayList<Integer> next=new ArrayList<>();
            if(x>0&&!seen[here-1])next.add(here-1);
            if(x<COLS-1&&!seen[here+1])next.add(here+1);
            if(z>0&&!seen[here-COLS])next.add(here-COLS);
            if(z<ROWS-1&&!seen[here+COLS])next.add(here+COLS);
            if(next.isEmpty())stack.pop();
            else {
                int there=next.get(random.nextInt(next.size()));
                seen[there]=true;
                edges.add(edge(here,there));
                stack.push(there);
            }
        }
        ArrayList<Long> alternatives=new ArrayList<>();
        for(int i=0;i<ROWS*COLS;i++) {
            int x=i%COLS,z=i/COLS;
            if(x<COLS-1&&!edges.contains(edge(i,i+1)))alternatives.add(edge(i,i+1));
            if(z<ROWS-1&&!edges.contains(edge(i,i+COLS)))alternatives.add(edge(i,i+COLS));
        }
        Collections.shuffle(alternatives,random);
        for(int i=0;i<Math.min(1+random.nextInt(3),alternatives.size());i++)edges.add(alternatives.get(i));
        for(long key:edges) {
            int a=(int)(key>>>32),b=(int)key;
            int ax=centersX[a/COLS][a%COLS], az=centersZ[a/COLS][a%COLS];
            int bx=centersX[b/COLS][b%COLS], bz=centersZ[b/COLS][b%COLS];
            if(a/COLS==b/COLS) {
                int mid=(ax+bx)/2;
                corridor(walk,ax,az,mid,az); corridor(walk,mid,az,mid,bz); corridor(walk,mid,bz,bx,bz);
            } else {
                int mid=(az+bz)/2;
                corridor(walk,ax,az,ax,mid); corridor(walk,ax,mid,bx,mid); corridor(walk,bx,mid,bx,bz);
            }
        }
        byte[] blocks=new byte[WIDTH*LENGTH*HEIGHT];
        for(int z=0;z<LENGTH;z++)for(int x=0;x<WIDTH;x++) {
            if(walk[z][x]) {
                put(blocks,x,0,z,random.nextInt(9)==0?4:3);
                put(blocks,x,7,z,1);
            } else if(touches(walk,x,z)) {
                put(blocks,x,0,z,6);
                for(int y=1;y<=7;y++)put(blocks,x,y,z,random.nextInt(7)==0?2:1);
            }
        }
        for(int z=0;z<ROWS;z++)for(int x=0;x<COLS;x++) {
            int cx=centersX[z][x],cz=centersZ[z][x];
            put(blocks,cx,0,cz,x==0&&z==1?7:x==3&&z==1?9:(x+z)%3==0?8:5);
            put(blocks,cx,7,cz,7);
        }
        int bx=centersX[1][3],bz=centersZ[1][3];
        for(int dx:new int[]{-4,4})for(int dz:new int[]{-4,4})
            for(int y=1;y<=5;y++)put(blocks,bx+dx,y,bz+dz,10);
        DungeonLayout result=new DungeonLayout(seed,WIDTH,HEIGHT,LENGTH,
            centersX[1][0],centersZ[1][0],bx,bz,blocks);
        if(!reachable(result))throw new IllegalStateException("unreachable room for seed "+seed);
        return result;
    }
    private static long edge(int a,int b) { return ((long)Math.min(a,b)<<32)|(Math.max(a,b)&0xffffffffL); }
    private static void put(byte[] b,int x,int y,int z,int value) {
        b[(y*LENGTH+z)*WIDTH+x]=(byte)value;
    }
    private static boolean touches(boolean[][] w,int x,int z) {
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
            int px=x+dx,pz=z+dz;
            if(px>=0&&px<WIDTH&&pz>=0&&pz<LENGTH&&w[pz][px])return true;
        }
        return false;
    }
    private static void corridor(boolean[][] w,int x1,int z1,int x2,int z2) {
        if(x1!=x2&&z1!=z2)throw new IllegalArgumentException("diagonal hallway");
        int steps=Math.max(Math.abs(x2-x1),Math.abs(z2-z1));
        for(int i=0;i<=steps;i++) {
            int x=x1+Integer.signum(x2-x1)*i,z=z1+Integer.signum(z2-z1)*i;
            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++) {
                int px=x+dx,pz=z+dz;
                if(Math.abs(dx)+Math.abs(dz)<=3&&px>0&&px<WIDTH-1&&pz>0&&pz<LENGTH-1)
                    w[pz][px]=true;
            }
        }
    }
    public static boolean reachable(DungeonLayout layout) {
        boolean[][] seen=new boolean[layout.length()][layout.width()];
        ArrayDeque<int[]> queue=new ArrayDeque<>();
        int sx=layout.entranceX(),sz=layout.entranceZ();
        if(!layout.passable(sx,sz))return false;
        queue.add(new int[]{sx,sz});seen[sz][sx]=true;
        while(!queue.isEmpty()) {
            int[] p=queue.remove();
            for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int x=p[0]+delta[0],z=p[1]+delta[1];
                if(x>=0&&x<layout.width()&&z>=0&&z<layout.length()
                    &&!seen[z][x]&&layout.passable(x,z)) {
                    seen[z][x]=true;queue.add(new int[]{x,z});
                }
            }
        }
        return seen[layout.bossZ()][layout.bossX()];
    }
}
